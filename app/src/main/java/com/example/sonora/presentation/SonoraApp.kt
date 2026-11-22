package com.example.sonora.presentation

import android.net.Uri
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.extractor.metadata.icy.IcyInfo
import com.example.sonora.domain.model.RadioStation
import com.example.sonora.alarm.RadioAlarmScheduler
import com.example.sonora.presentation.localization.uiText
import com.example.sonora.player.PlayerConnection
import com.example.sonora.presentation.components.StationArtwork
import com.example.sonora.presentation.screens.EqualizerScreen
import com.example.sonora.presentation.screens.HomeScreen
import com.example.sonora.presentation.screens.LibraryScreen
import com.example.sonora.presentation.screens.LanguageSetupScreen
import com.example.sonora.presentation.screens.LoadingSettingsScreen
import com.example.sonora.presentation.screens.PlayerSheet
import com.example.sonora.presentation.screens.SearchScreen
import com.example.sonora.presentation.screens.SettingsScreen
import com.example.sonora.presentation.theme.SonoraBackground
import com.example.sonora.presentation.theme.SonoraMuted
import com.example.sonora.presentation.theme.SonoraPrimary
import com.example.sonora.presentation.theme.SonoraSurfaceElevated
import com.example.sonora.presentation.theme.SonoraText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun bottomDestinations(language: com.example.sonora.domain.model.RadioLanguage?) = listOf(
    BottomDestination("home", uiText(language, "nav.home"), Icons.Filled.Home),
    BottomDestination("search", uiText(language, "nav.search"), Icons.Filled.Search),
    BottomDestination("library", uiText(language, "nav.library"), Icons.Filled.Favorite),
    BottomDestination("settings", uiText(language, "nav.settings"), Icons.Filled.Settings)
)

/** Returns (track title, artist) from a radio StreamTitle field. */
private fun parseIcyStreamTitle(rawValue: String): Pair<String?, String?> {
    val cleaned = rawValue
        .trim()
        .replace(Regex("(?i)^StreamTitle\\s*=\\s*"), "")
        .trim()
        .trim('\'', '"', ';')
        .replace(Regex("\\s+"), " ")

    if (cleaned.isBlank()) return null to null

    // Some stations use "Track by Artist" instead of the usual "Artist - Track".
    val byMatch = Regex("(?i)^(.+?)\\s+by\\s+(.+)$").matchEntire(cleaned)
    if (byMatch != null) {
        return byMatch.groupValues[1].trim().takeIf { it.isNotBlank() } to
            byMatch.groupValues[2].trim().takeIf { it.isNotBlank() }
    }

    // Most ICY radio streams use "Artist - Track". Also accept en/em dashes and "|".
    val parts = cleaned.split(Regex("\\s(?:-|–|—|\\|)\\s"), limit = 2)
    return if (parts.size == 2) {
        parts[1].trim().takeIf { it.isNotBlank() } to
            parts[0].trim().takeIf { it.isNotBlank() }
    } else {
        cleaned to null
    }
}

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun SonoraApp(viewModel: SonoraViewModel) {
    val state by viewModel.uiState.collectAsState()

    if (!state.isSettingsLoaded) {
        LoadingSettingsScreen()
        return
    }
    if (state.radioLanguage == null) {
        LanguageSetupScreen(onLanguageSelected = viewModel::setRadioLanguage)
        return
    }

    val selectedLanguage = state.radioLanguage
    val appContext = LocalContext.current

    LaunchedEffect(selectedLanguage, appContext) {
        selectedLanguage?.let { language ->
            appContext.getSharedPreferences("sonora_player", Context.MODE_PRIVATE)
                .edit()
                .putString("radio_language", language.code)
                .apply()
        }
    }
    var radioAlarmConfig by remember(appContext) {
        mutableStateOf(RadioAlarmScheduler.load(appContext))
    }
    DisposableEffect(appContext) {
        val alarmConfigReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                radioAlarmConfig = RadioAlarmScheduler.load(appContext)
            }
        }
        ContextCompat.registerReceiver(
            appContext,
            alarmConfigReceiver,
            IntentFilter(RadioAlarmScheduler.ACTION_ALARM_CONFIG_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            runCatching { appContext.unregisterReceiver(alarmConfigReceiver) }
        }
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val playerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val controller = PlayerConnection.controller

    // If the radio alarm started playback while the app was closed, restore
    // the station in the app UI as soon as the MediaController reconnects.
    LaunchedEffect(controller, radioAlarmConfig) {
        val controllerUri = controller?.currentMediaItem?.localConfiguration?.uri?.toString()
        val alarmStation = radioAlarmConfig?.station
        if (controller?.playWhenReady == true &&
            alarmStation != null &&
            controllerUri == alarmStation.streamUrl
        ) {
            viewModel.onStationSelected(alarmStation)
        }
    }

    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var isPlaying by remember(controller) { mutableStateOf(controller?.isPlaying == true) }
    var isBuffering by remember(controller) { mutableStateOf(controller?.playbackState == Player.STATE_BUFFERING) }
    var playbackError by remember(controller) { mutableStateOf<String?>(null) }
    var sleepTimerMinutes by rememberSaveable { mutableStateOf(0) }
    var playbackQueue by remember { mutableStateOf<List<RadioStation>>(emptyList()) }
    var queueIndex by remember { mutableStateOf(0) }

    // Live metadata may arrive later than the station connection itself.
    var nowPlayingTitle by remember { mutableStateOf<String?>(null) }
    var nowPlayingArtist by remember { mutableStateOf<String?>(null) }
    var nowPlayingArtworkUri by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var autoReconnectEnabled by remember { mutableStateOf(true) }
    var reconnectJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleReconnect() {
        if (!autoReconnectEnabled || reconnectJob?.isActive == true) return

        reconnectJob = coroutineScope.launch {
            var attempt = 0
            while (autoReconnectEnabled) {
                val waitMs = when (attempt) {
                    0 -> 1_500L
                    1 -> 3_500L
                    2 -> 7_000L
                    3 -> 15_000L
                    else -> 30_000L
                }
                attempt++

                isBuffering = true
                playbackError = uiText(selectedLanguage, "error.reconnecting", attempt)
                delay(waitMs)

                if (!autoReconnectEnabled) return@launch
                val player = PlayerConnection.controller ?: return@launch
                player.prepare()
                player.play()

                // Keep the selected station and retry with a capped backoff while
                // the user still wants playback. A successful start cancels this job.
                delay(8_000L)
                if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                    playbackError = null
                    isBuffering = false
                    return@launch
                }
            }
        }
    }

    DisposableEffect(controller, radioAlarmConfig, state.currentStation?.id, playbackQueue) {
        if (controller == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Never leave the previous station's song/artist on the player.
                    nowPlayingTitle = null
                    nowPlayingArtist = null
                    nowPlayingArtworkUri = mediaItem?.mediaMetadata?.artworkUri?.toString()

                    val mediaId = mediaItem?.mediaId.orEmpty()
                    val queuedStationIndex = playbackQueue.indexOfFirst { it.id == mediaId }
                    if (queuedStationIndex >= 0) {
                        val station = playbackQueue[queuedStationIndex]
                        queueIndex = queuedStationIndex
                        if (state.currentStation?.id != station.id) {
                            viewModel.onStationSelected(station)
                        }
                        appContext.getSharedPreferences("sonora_player", Context.MODE_PRIVATE)
                            .edit()
                            .putString("current_station_name", station.name)
                            .putInt("current_station_bitrate", station.bitrate)
                            .putString("current_station_codec", station.codec)
                            .apply()
                    }
                    val artist = mediaItem?.mediaMetadata?.artist?.toString().orEmpty()
                    val isAlarmStream = mediaId.startsWith(RadioAlarmScheduler.ALARM_MEDIA_ID_PREFIX) ||
                        artist.contains("Радиобудильник", ignoreCase = true) ||
                        artist.contains("Radio alarm", ignoreCase = true)
                    val currentUri = mediaItem?.localConfiguration?.uri?.toString()

                    if (isAlarmStream) {
                        // The service owns retries while an alarm is ringing.
                        autoReconnectEnabled = false
                        reconnectJob?.cancel()
                        reconnectJob = null

                        if (!currentUri.isNullOrBlank()) {
                            val savedStation = radioAlarmConfig?.station
                                ?.takeIf { it.streamUrl == currentUri }
                            val station = savedStation ?: RadioStation(
                                id = currentUri,
                                name = mediaId
                                    .takeIf { it.startsWith(RadioAlarmScheduler.ALARM_MEDIA_ID_PREFIX) }
                                    ?.removePrefix(RadioAlarmScheduler.ALARM_MEDIA_ID_PREFIX)
                                    ?.takeIf { it.isNotBlank() }
                                    ?: mediaItem?.mediaMetadata?.title?.toString()
                                        ?.takeIf { it.isNotBlank() }
                                    ?: "SONARA Radio",
                                streamUrl = currentUri,
                                favicon = mediaItem?.mediaMetadata?.artworkUri?.toString().orEmpty(),
                                country = "",
                                countryCode = "",
                                language = "",
                                tags = "",
                                codec = "",
                                bitrate = 0,
                                homepage = ""
                            )
                            viewModel.onStationSelected(station)
                        }
                        return
                    }

                    val alarmStation = radioAlarmConfig?.station ?: return
                    if (currentUri == alarmStation.streamUrl) {
                        viewModel.onStationSelected(alarmStation)
                    }
                }

                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    isPlaying = isPlayingNow
                    if (isPlayingNow) {
                        reconnectJob?.cancel()
                        reconnectJob = null
                        playbackError = null
                        isBuffering = false
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY && controller.isPlaying) {
                        playbackError = null
                    }
                }

                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    val station = state.currentStation
                    val candidateTitle = mediaMetadata.title?.toString()?.trim()
                    val isStationTitle = candidateTitle.isNullOrBlank() ||
                        candidateTitle.equals(station?.name, ignoreCase = true) ||
                        candidateTitle.equals("SONARA Radio", ignoreCase = true)

                    val candidateArtist = (mediaMetadata.artist ?: mediaMetadata.albumArtist)
                        ?.toString()
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank() &&
                                !it.equals(station?.subtitle, ignoreCase = true) &&
                                !it.startsWith("SONARA", ignoreCase = true)
                        }

                    if (!isStationTitle) {
                        val (parsedTitle, parsedArtist) = parseIcyStreamTitle(candidateTitle.orEmpty())
                        nowPlayingTitle = parsedTitle ?: candidateTitle
                        // ICY often puts both values into one "Artist - Track" title.
                        // Prefer that parsed artist, then a separate MediaMetadata artist.
                        nowPlayingArtist = parsedArtist ?: candidateArtist
                    } else if (candidateArtist != null) {
                        nowPlayingArtist = candidateArtist
                    }

                    mediaMetadata.artworkUri?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { nowPlayingArtworkUri = it }
                }

                override fun onMetadata(metadata: Metadata) {
                    var latestTrack: Pair<String?, String?>? = null
                    for (index in 0 until metadata.length()) {
                        val icyInfo = metadata[index] as? IcyInfo ?: continue
                        val rawTitle = icyInfo.title?.trim().orEmpty()
                        if (rawTitle.isBlank()) continue
                        latestTrack = parseIcyStreamTitle(rawTitle)
                    }

                    latestTrack?.first?.takeIf { it.isNotBlank() }?.let { title ->
                        nowPlayingTitle = title
                        // An ICY update without an artist must not retain the previous song's artist.
                        nowPlayingArtist = latestTrack?.second?.takeIf { it.isNotBlank() }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    playbackError = error.message ?: uiText(selectedLanguage, "error.connection_failed")
                    isPlaying = false
                    isBuffering = autoReconnectEnabled
                    scheduleReconnect()
                }
            }
            controller.addListener(listener)
            // Read the current item metadata immediately in case playback started before
            // this screen attached its listener.
            listener.onMediaMetadataChanged(controller.mediaMetadata)
            onDispose { controller.removeListener(listener) }
        }
    }

    LaunchedEffect(sleepTimerMinutes) {
        if (sleepTimerMinutes > 0) {
            delay(sleepTimerMinutes * 60_000L)
            autoReconnectEnabled = false
            reconnectJob?.cancel()
            reconnectJob = null
            PlayerConnection.controller?.pause()
            sleepTimerMinutes = 0
        }
    }

    fun createRadioMediaItem(station: RadioStation): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setStation(station.name)
            .setAlbumTitle(station.name)
            .setSubtitle(station.name)
            .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .setExtras(
                Bundle().apply {
                    putInt("sonara_bitrate", station.bitrate)
                    putString("sonara_codec", station.codec)
                }
            )
            .apply {
                if (station.favicon.isNotBlank()) setArtworkUri(Uri.parse(station.favicon))
            }
            .build()

        return MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(station.streamUrl)
            // Keep station metadata separate from live ICY track title/artist.
            .setMediaMetadata(metadata)
            .build()
    }

    fun playStation(station: RadioStation, sourceQueue: List<RadioStation>) {
        val distinctQueue = sourceQueue.distinctBy { it.id }.let { queue ->
            if (queue.any { it.id == station.id }) queue else queue + station
        }
        val selectedIndex = distinctQueue.indexOfFirst { it.id == station.id }.coerceAtLeast(0)
        playbackQueue = distinctQueue
        queueIndex = selectedIndex

        appContext.getSharedPreferences("sonora_player", Context.MODE_PRIVATE)
            .edit()
            .putString("current_station_name", station.name)
            .putInt("current_station_bitrate", station.bitrate)
            .putString("current_station_codec", station.codec)
            .apply()
        viewModel.onStationSelected(station)
        reconnectJob?.cancel()
        reconnectJob = null
        autoReconnectEnabled = true
        playbackError = null
        isBuffering = true
        nowPlayingTitle = null
        nowPlayingArtist = null
        nowPlayingArtworkUri = station.favicon.takeIf { it.isNotBlank() }

        val mediaController = PlayerConnection.controller
        if (mediaController != null) {
            // Keep the full station queue inside the MediaSession/ExoPlayer.
            // Android's media notification and lock-screen controls can then use
            // the standard Previous/Next media-item commands while the app is closed.
            val queueItems = distinctQueue.map(::createRadioMediaItem)
            if (mediaController.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) {
                mediaController.setMediaItems(queueItems, selectedIndex, C.TIME_UNSET)
            } else {
                // Compatibility fallback for a controller with restricted playlist commands.
                mediaController.setMediaItem(queueItems[selectedIndex])
            }
            if (mediaController.isCommandAvailable(Player.COMMAND_SET_REPEAT_MODE)) {
                mediaController.repeatMode = Player.REPEAT_MODE_ALL
            }
            mediaController.prepare()
            mediaController.play()
        } else {
            playbackError = uiText(selectedLanguage, "error.player_connecting")
        }
        showPlayer = true
    }

    fun startStation(station: RadioStation, sourceQueue: List<RadioStation>) {
        playStation(station, sourceQueue)
    }

    fun changeStation(direction: Int) {
        if (playbackQueue.size < 2) return
        val player = PlayerConnection.controller ?: return

        val canSkip = if (direction < 0) {
            player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        } else {
            player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        }

        if (player.mediaItemCount > 1 && canSkip) {
            if (direction < 0) {
                player.seekToPreviousMediaItem()
            } else {
                player.seekToNextMediaItem()
            }
        } else {
            val nextIndex = (queueIndex + direction + playbackQueue.size) % playbackQueue.size
            playStation(playbackQueue[nextIndex], playbackQueue)
        }
    }

    fun togglePlayback() {
        val mediaController = PlayerConnection.controller ?: return
        val shouldPause = mediaController.isPlaying ||
            (mediaController.playWhenReady && mediaController.playbackState != Player.STATE_IDLE)

        if (shouldPause) {
            autoReconnectEnabled = false
            reconnectJob?.cancel()
            reconnectJob = null
            mediaController.pause()
        } else {
            autoReconnectEnabled = true
            reconnectJob?.cancel()
            reconnectJob = null
            if (mediaController.playbackState == Player.STATE_IDLE ||
                mediaController.playbackState == Player.STATE_ENDED
            ) {
                mediaController.prepare()
            }
            mediaController.play()
        }
    }

    Scaffold(
        containerColor = SonoraBackground,
        bottomBar = {
            Column {
                state.currentStation?.let { station ->
                    MiniPlayer(
                        station = station,
                        language = selectedLanguage,
                        trackTitle = nowPlayingTitle,
                        trackArtist = nowPlayingArtist,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        onClick = { showPlayer = true },
                        onTogglePlayback = ::togglePlayback
                    )
                }
                NavigationBar(
                    containerColor = SonoraBackground,
                    tonalElevation = 0.dp
                ) {
                    bottomDestinations(selectedLanguage).forEach { destination ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == destination.route || (destination.route == "settings" && it.route == "equalizer") } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                // If this destination is already in the back stack,
                                // return to it directly. Otherwise replace the current
                                // top-level screen above Home with the requested one.
                                val returnedToExistingDestination =
                                    navController.popBackStack(destination.route, inclusive = false)

                                if (!returnedToExistingDestination) {
                                    navController.navigate(destination.route) {
                                        popUpTo("home") {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SonoraPrimary,
                                selectedTextColor = SonoraPrimary,
                                unselectedIconColor = SonoraMuted,
                                unselectedTextColor = SonoraMuted,
                                indicatorColor = SonoraPrimary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    state = state,
                    onSearchClick = { navController.navigate("search") },
                    onStationClick = { station -> startStation(station, state.popularStations) },
                    onFavoriteClick = viewModel::toggleFavorite,
                    onGenreClick = viewModel::loadGenre,
                    onRefresh = {
                        val selectedGenre = state.selectedGenre
                        if (selectedGenre == null) viewModel.loadPopular()
                        else viewModel.loadGenre(selectedGenre)
                    },
                    onLoadMore = viewModel::loadMoreStations
                )
            }
            composable("search") {
                SearchScreen(
                    state = state,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onStationClick = { station -> startStation(station, state.searchResults) },
                    onFavoriteClick = viewModel::toggleFavorite,
                    onLoadMore = viewModel::loadMoreSearchResults
                )
            }
            composable("library") {
                LibraryScreen(
                    state = state,
                    onStationClick = { station, queue -> startStation(station, queue) },
                    onFavoriteClick = viewModel::toggleFavorite,
                    onClearHistory = viewModel::clearHistory
                )
            }
            composable("settings") {
                SettingsScreen(
                    state = state,
                    sleepTimerMinutes = sleepTimerMinutes,
                    onBufferModeChange = viewModel::setBufferMode,
                    onSleepTimerChange = { sleepTimerMinutes = it },
                    onOpenEqualizer = { navController.navigate("equalizer") },
                    onThemeChange = viewModel::setAppTheme,
                    onRadioLanguageChange = viewModel::setRadioLanguage,
                    alarmConfig = radioAlarmConfig,
                    onSetAlarm = { hour, minute, station, repeatDaily ->
                        val scheduled = RadioAlarmScheduler.schedule(appContext, hour, minute, station, repeatDaily)
                        if (scheduled) {
                            radioAlarmConfig = RadioAlarmScheduler.load(appContext)
                        }
                        scheduled
                    },
                    onCancelAlarm = {
                        RadioAlarmScheduler.cancel(appContext)
                        radioAlarmConfig = null
                    },
                    onRequestExactAlarmPermission = {
                        RadioAlarmScheduler.requestExactAlarmPermission(appContext)
                    }
                )
            }
            composable("equalizer") {
                EqualizerScreen(
                    settings = state.equalizerSettings,
                    onSettingsChange = viewModel::setEqualizerSettings,
                    onBack = { navController.popBackStack() },
                    language = selectedLanguage
                )
            }
        }
    }

    val currentStation = state.currentStation
    if (showPlayer && currentStation != null) {
        ModalBottomSheet(
            onDismissRequest = { showPlayer = false },
            sheetState = playerSheetState,
            containerColor = SonoraBackground,
            contentColor = SonoraText,
            dragHandle = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 7.dp, bottom = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(SonoraMuted.copy(alpha = 0.72f))
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = uiText(selectedLanguage, "player.collapse"),
                        tint = SonoraMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        ) {
            PlayerSheet(
                station = currentStation,
                language = selectedLanguage,
                trackTitle = nowPlayingTitle,
                trackArtist = nowPlayingArtist,
                trackArtworkUri = nowPlayingArtworkUri,
                isFavorite = state.favorites.any { it.id == currentStation.id },
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                error = playbackError,
                queuePosition = if (playbackQueue.isEmpty()) 1 else queueIndex + 1,
                queueSize = playbackQueue.size,
                onTogglePlayback = ::togglePlayback,
                onPreviousStation = { changeStation(-1) },
                onNextStation = { changeStation(1) },
                onToggleFavorite = { viewModel.toggleFavorite(currentStation) },
                onClose = { showPlayer = false }
            )
        }
    }
}

@Composable
private fun MiniPlayer(
    station: RadioStation,
    language: com.example.sonora.domain.model.RadioLanguage?,
    trackTitle: String?,
    trackArtist: String?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    onTogglePlayback: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SonoraSurfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StationArtwork(station, Modifier.size(42.dp), 12.dp)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(trackTitle?.takeIf { it.isNotBlank() } ?: station.name, color = SonoraText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    isBuffering -> uiText(language, "mini.connecting")
                    !trackTitle.isNullOrBlank() -> {
                        val artist = trackArtist?.takeIf { it.isNotBlank() }
                        if (artist != null) "$artist · ${station.name}" else station.name
                    }
                    else -> "${uiText(language, "mini.live")} · ${station.country.ifBlank { uiText(language, "mini.radio") }}"
                },
                color = SonoraMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onTogglePlayback, modifier = Modifier.size(42.dp)) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) uiText(language, "player.pause") else uiText(language, "player.play"),
                tint = SonoraPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

