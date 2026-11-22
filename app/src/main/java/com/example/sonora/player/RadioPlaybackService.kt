package com.example.sonora.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.MediaSessionService
import com.example.sonora.MainActivity
import com.example.sonora.R
import com.example.sonora.domain.model.RadioLanguage
import com.example.sonora.presentation.localization.uiText
import com.example.sonora.alarm.RadioAlarmScheduler
import com.example.sonora.data.settings.BufferMode
import com.example.sonora.data.settings.EqualizerContract
import kotlin.math.abs
import kotlin.math.roundToInt

/** Owns the radio player and its audio effects while playback is active. */
@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var equalizerSessionId: Int = 0
    private var receiverRegistered = false
    private var alarmPlaybackActive = false
    private var alarmStationName: String = "SONARA Radio"
    private val reconnectHandler = Handler(Looper.getMainLooper())
    private var reconnectRunnable: Runnable? = null
    private var reconnectAttempt = 0
    private val adaptiveBufferController = AdaptiveBufferController()
    private var hasReachedReadyForCurrentItem = false

    private val equalizerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            applyEqualizerSettings()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createAlarmNotificationChannel()

        val notificationProvider = SonaraMediaNotificationProvider(this)
            .apply { setSmallIcon(R.drawable.ic_stat_sonara) }
        setMediaNotificationProvider(notificationProvider)

        val modeName = getSharedPreferences("sonora_player", Context.MODE_PRIVATE)
            .getString("buffer_mode", BufferMode.AUTO.name)
        val mode = BufferMode.fromName(modeName)
        val loadControl: androidx.media3.exoplayer.LoadControl = if (mode == BufferMode.AUTO) {
            val delegate = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    90_000,
                    90_000,
                    mode.playbackBufferMs,
                    mode.rebufferMs
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            AdaptiveRadioLoadControl(delegate, adaptiveBufferController)
        } else {
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    mode.minBufferMs,
                    mode.maxBufferMs,
                    mode.playbackBufferMs,
                    mode.rebufferMs
                )
                .build()
        }

        // Media3's adaptive selector uses measured bandwidth to choose between
        // available HLS audio variants. Single-bitrate MP3/AAC streams cannot
        // change bitrate, so the adaptive load control protects them with buffering.
        val trackSelector = DefaultTrackSelector(this, AdaptiveTrackSelection.Factory()).apply {
            setParameters(
                buildUponParameters()
                    .setForceHighestSupportedBitrate(false)
                    .setForceLowestBitrate(false)
                    .build()
            )
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            // Keep only the CPU awake while ExoPlayer is actively playing.
            // The wake lock is released automatically when playback stops.
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
                setHandleAudioBecomingNoisy(true)
            }

        exoPlayer = player
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long
            ) {
                if (mode != BufferMode.AUTO) return
                val stationBitrateKbps = getSharedPreferences("sonora_player", Context.MODE_PRIVATE)
                    .getInt("current_station_bitrate", 0)
                adaptiveBufferController.updateBandwidthEstimate(
                    bitrateEstimate,
                    stationBitrateKbps
                )
            }
        })
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId > 0) ensureEqualizerAttached()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    ensureEqualizerAttached()
                    hasReachedReadyForCurrentItem = true
                }
                if (
                    playbackState == Player.STATE_BUFFERING &&
                    hasReachedReadyForCurrentItem &&
                    player.playWhenReady &&
                    mode == BufferMode.AUTO
                ) {
                    adaptiveBufferController.onRebuffer()
                }
                if (playbackState == Player.STATE_ENDED && alarmPlaybackActive) {
                    scheduleAlarmReconnect()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    ensureEqualizerAttached()
                    reconnectAttempt = 0
                    cancelAlarmReconnect()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                adaptiveBufferController.resetForNewStation()
                hasReachedReadyForCurrentItem = false

                // The active station can change from the lock screen or notification
                // while the Activity is not running. Keep the notification fallback
                // metadata in sync with the MediaSession playlist item.
                if (!alarmPlaybackActive && mediaItem != null) {
                    val metadata = mediaItem.mediaMetadata
                    val stationName = metadata.station?.toString()?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: metadata.albumTitle?.toString()?.trim()
                            ?.takeIf { it.isNotBlank() }
                    val preferences = getSharedPreferences("sonora_player", Context.MODE_PRIVATE)
                    val extras = metadata.extras
                    preferences.edit().apply {
                        if (stationName != null) putString("current_station_name", stationName)
                        if (extras != null && extras.containsKey("sonara_bitrate")) {
                            putInt("current_station_bitrate", extras.getInt("sonara_bitrate"))
                        }
                        if (extras != null && extras.containsKey("sonara_codec")) {
                            putString("current_station_codec", extras.getString("sonara_codec"))
                        }
                    }.apply()
                }

                if (!alarmPlaybackActive || mediaItem == null) return
                val legacyArtist = mediaItem.mediaMetadata.artist?.toString().orEmpty()
                val isAlarmItem =
                    mediaItem.mediaId.startsWith(RadioAlarmScheduler.ALARM_MEDIA_ID_PREFIX) ||
                        legacyArtist.contains("Радиобудильник", ignoreCase = true)
                if (!isAlarmItem) {
                    // If the user chooses another station in SONARA, hand playback
                    // back to the normal Media3 notification and clear alarm state.
                    alarmPlaybackActive = false
                    cancelAlarmReconnect()
                    clearAlarmPlaybackState()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    triggerNotificationUpdate()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (alarmPlaybackActive) scheduleAlarmReconnect()
            }
        })

        ContextCompat.registerReceiver(
            this,
            equalizerReceiver,
            IntentFilter(EqualizerContract.ACTION_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(createAppPendingIntent())
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            RadioAlarmScheduler.ACTION_PLAY_ALARM -> {
                val streamUrl = intent.getStringExtra(RadioAlarmScheduler.EXTRA_STREAM_URL)
                val stationName = intent.getStringExtra(RadioAlarmScheduler.EXTRA_STATION_NAME)
                    ?.takeIf { it.isNotBlank() } ?: "SONARA Radio"
                val artwork = intent.getStringExtra(RadioAlarmScheduler.EXTRA_STATION_ARTWORK)
                    ?.takeIf { it.isNotBlank() }

                if (!streamUrl.isNullOrBlank()) {
                    startAlarmPlayback(streamUrl, stationName, artwork)
                    return START_STICKY
                }
            }

            RadioAlarmScheduler.ACTION_STOP_ALARM -> {
                stopAlarmPlayback()
                return START_NOT_STICKY
            }

            null -> {
                // Android may recreate a sticky foreground service after reclaiming memory.
                // Restore the active alarm stream instead of leaving the user in silence.
                val saved = loadAlarmPlaybackState()
                if (saved != null) {
                    startAlarmPlayback(
                        streamUrl = saved.first,
                        stationName = saved.second,
                        artwork = saved.third
                    )
                    return START_STICKY
                }
            }
        }

        return result
    }

    private fun startAlarmPlayback(streamUrl: String, stationName: String, artwork: String?) {
        alarmPlaybackActive = true
        alarmStationName = stationName
        reconnectAttempt = 0
        cancelAlarmReconnect()
        saveAlarmPlaybackState(streamUrl, stationName, artwork)

        // Promote immediately. The alarm service must not be killed by Android's
        // foreground-service start timeout while the stream is buffering.
        showAlarmForegroundNotification(stationName)

        val metadata = MediaMetadata.Builder()
            .setStation(stationName)
            .setAlbumTitle(stationName)
            .setSubtitle(stationName)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .apply {
                artwork?.let { setArtworkUri(Uri.parse(it)) }
            }
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId("${RadioAlarmScheduler.ALARM_MEDIA_ID_PREFIX}$stationName")
            .setUri(streamUrl)
            // Do not pin stationName as MediaMetadata.title: timed ICY metadata
            // must be allowed to replace it with the actual song title.
            .setMediaMetadata(metadata)
            .build()

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    private fun stopAlarmPlayback() {
        cancelAlarmReconnect()
        // Clear the sticky-restart marker before stopping the service.
        clearAlarmPlaybackState()

        exoPlayer?.apply {
            stop()
            clearMediaItems()
        }
        alarmPlaybackActive = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        triggerNotificationUpdate()
        stopSelf()
    }

    private fun scheduleAlarmReconnect() {
        if (!alarmPlaybackActive || reconnectRunnable != null) return

        val delays = longArrayOf(1_500L, 3_500L, 7_000L, 15_000L, 30_000L)
        val waitMs = delays[reconnectAttempt.coerceAtMost(delays.lastIndex)]
        reconnectAttempt++

        val retry = Runnable {
            reconnectRunnable = null
            if (!alarmPlaybackActive) return@Runnable
            exoPlayer?.apply {
                prepare()
                playWhenReady = true
            }
        }
        reconnectRunnable = retry
        reconnectHandler.postDelayed(retry, waitMs)
    }

    private fun cancelAlarmReconnect() {
        reconnectRunnable?.let { reconnectHandler.removeCallbacks(it) }
        reconnectRunnable = null
    }

    private fun saveAlarmPlaybackState(
        streamUrl: String,
        stationName: String,
        artwork: String?
    ) {
        getSharedPreferences(ALARM_PLAYBACK_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("stream_url", streamUrl)
            .putString("station_name", stationName)
            .putString("artwork", artwork)
            .putBoolean("active", true)
            .commit()
    }

    private fun clearAlarmPlaybackState() {
        getSharedPreferences(ALARM_PLAYBACK_PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun loadAlarmPlaybackState(): Triple<String, String, String?>? {
        val preferences = getSharedPreferences(ALARM_PLAYBACK_PREFS, Context.MODE_PRIVATE)
        if (!preferences.getBoolean("active", false)) return null
        val streamUrl = preferences.getString("stream_url", null)?.takeIf { it.isNotBlank() }
            ?: return null
        val stationName = preferences.getString("station_name", null)
            ?.takeIf { it.isNotBlank() } ?: "SONARA Radio"
        val artwork = preferences.getString("artwork", null)
        return Triple(streamUrl, stationName, artwork)
    }

    override fun onUpdateNotification(
        session: MediaSession,
        startInForegroundRequired: Boolean
    ) {
        if (alarmPlaybackActive) {
            // Keep one persistent media-style foreground notification with a Stop action.
            // Media3 would otherwise replace the alarm notification with its default card.
            showAlarmForegroundNotification(alarmStationName)
            return
        }
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    private fun selectedRadioLanguage(): RadioLanguage? {
        val code = getSharedPreferences("sonora_player", Context.MODE_PRIVATE)
            .getString("radio_language", null)
        return RadioLanguage.fromName(code)
    }

    private fun createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            uiText(selectedRadioLanguage(), "alarm.channel_name"),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = uiText(selectedRadioLanguage(), "alarm.channel_description")
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun showAlarmForegroundNotification(stationName: String) {
        val stopIntent = Intent(this, RadioPlaybackService::class.java).apply {
            action = RadioAlarmScheduler.ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            STOP_ALARM_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sonara)
            .setContentTitle(stationName)
            .setContentText(uiText(selectedRadioLanguage(), "alarm.notification_text"))
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(createAppPendingIntent())
            .setStyle(
                MediaStyleNotificationHelper.MediaStyle(checkNotNull(mediaSession))
                    .setShowActionsInCompactView(0)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                uiText(selectedRadioLanguage(), "alarm.stop_action"),
                stopPendingIntent
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ALARM_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(ALARM_NOTIFICATION_ID, notification)
        }
    }

    private fun createAppPendingIntent(): PendingIntent {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this,
            SESSION_ACTIVITY_REQUEST_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureEqualizerAttached() {
        val sessionId = exoPlayer?.audioSessionId ?: return
        // Audio session IDs are positive after ExoPlayer has initialized its audio output.
        if (sessionId <= 0) return

        try {
            if (equalizer == null || equalizerSessionId != sessionId) {
                releaseEqualizer()
                equalizer = Equalizer(0, sessionId)
                equalizerSessionId = sessionId
            }
            applyEqualizerSettings()
        } catch (_: Exception) {
            // Some Android devices do not expose a system equalizer implementation.
            releaseEqualizer()
        }
    }

    private fun applyEqualizerSettings() {
        val effect = equalizer ?: run {
            ensureEqualizerAttached()
            return
        }

        val preferences = getSharedPreferences(EqualizerContract.PREFERENCES, Context.MODE_PRIVATE)
        val enabled = preferences.getBoolean(EqualizerContract.ENABLED_KEY, true)
        val levels = List(5) { index ->
            preferences.getFloat("${EqualizerContract.BAND_KEY_PREFIX}$index", 0f)
                .coerceIn(-12f, 12f)
        }

        try {
            val frequencyTargets = intArrayOf(60, 230, 910, 3_600, 14_000)
            val range = effect.bandLevelRange
            val bandCount = effect.numberOfBands.toInt()

            for (band in 0 until bandCount) {
                val centerFrequencyHz = effect.getCenterFreq(band.toShort()) / 1_000
                val targetIndex = frequencyTargets.indices.minByOrNull { index ->
                    abs(frequencyTargets[index] - centerFrequencyHz)
                } ?: continue
                val millibels = (levels[targetIndex] * 100f).roundToInt()
                    .coerceIn(range[0].toInt(), range[1].toInt())
                effect.setBandLevel(band.toShort(), millibels.toShort())
            }
            effect.enabled = enabled
        } catch (_: Exception) {
            releaseEqualizer()
        }
    }

    private fun releaseEqualizer() {
        runCatching { equalizer?.release() }
        equalizer = null
        equalizerSessionId = 0
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        alarmPlaybackActive = false
        cancelAlarmReconnect()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(equalizerReceiver) }
            receiverRegistered = false
        }
        releaseEqualizer()
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    private companion object {
        const val PLAYBACK_CHANNEL_ID = "sonara_playback"
        const val ALARM_CHANNEL_ID = "sonara_alarm"
        const val ALARM_NOTIFICATION_ID = 7312
        const val STOP_ALARM_REQUEST_CODE = 7313
        const val ALARM_PLAYBACK_PREFS = "sonara_alarm_playback_state"
        const val SESSION_ACTIVITY_REQUEST_CODE = 7301
    }
}

/**
 * Media3 normally places only the performer in the notification's second line.
 * SONARA adds the current radio station there as well. The track title remains
 * the notification title, and the station is kept in MediaMetadata.station/albumTitle.
 */
@OptIn(UnstableApi::class)
private class SonaraMediaNotificationProvider(private val context: Context) :
    DefaultMediaNotificationProvider(
        context,
        DefaultMediaNotificationProvider.NotificationIdProvider {
            DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID
        },
        "sonara_playback",
        R.string.playback_notification_channel
    ) {

    override fun getNotificationContentText(metadata: MediaMetadata): CharSequence? {
        val artist = metadata.artist?.toString()?.trim()
            ?.takeIf { it.isNotBlank() && !it.startsWith("SONARA", ignoreCase = true) }
        val station = metadata.station?.toString()?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: metadata.albumTitle?.toString()?.trim()?.takeIf { it.isNotBlank() }
            ?: context.getSharedPreferences("sonora_player", Context.MODE_PRIVATE)
                .getString("current_station_name", null)
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        return listOfNotNull(
            artist,
            station?.takeUnless { it.equals(artist, ignoreCase = true) }
        ).joinToString(" · ").takeIf { it.isNotBlank() }
    }

    override fun getNotificationContentTitle(metadata: MediaMetadata): CharSequence? {
        return metadata.title?.toString()?.trim()?.takeIf { it.isNotBlank() }
            ?: metadata.displayTitle?.toString()?.trim()?.takeIf { it.isNotBlank() }
            ?: metadata.station?.toString()?.trim()?.takeIf { it.isNotBlank() }
            ?: metadata.albumTitle?.toString()?.trim()?.takeIf { it.isNotBlank() }
            ?: "SONARA"
    }
}

