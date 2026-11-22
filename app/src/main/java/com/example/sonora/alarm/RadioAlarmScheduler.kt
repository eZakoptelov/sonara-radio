package com.example.sonora.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.sonora.MainActivity
import com.example.sonora.domain.model.RadioStation
import java.util.Calendar

data class RadioAlarmConfig(
    val hour: Int,
    val minute: Int,
    val station: RadioStation,
    val repeatDaily: Boolean
)

object RadioAlarmScheduler {
    const val ACTION_PLAY_ALARM = "com.example.sonora.action.PLAY_RADIO_ALARM"
    const val ACTION_STOP_ALARM = "com.example.sonora.action.STOP_RADIO_ALARM"
    const val ACTION_ALARM_CONFIG_CHANGED = "com.example.sonora.action.ALARM_CONFIG_CHANGED"
    const val ALARM_MEDIA_ID_PREFIX = "sonara_radio_alarm:"
    const val EXTRA_STREAM_URL = "alarm_stream_url"
    const val EXTRA_STATION_NAME = "alarm_station_name"
    const val EXTRA_STATION_ARTWORK = "alarm_station_artwork"

    private const val PREFS_NAME = "sonara_radio_alarm"
    private const val REQUEST_CODE = 7310
    private const val SHOW_REQUEST_CODE = 7311

    fun load(context: Context): RadioAlarmConfig? {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!preferences.getBoolean("enabled", false)) return null
        return loadSavedConfig(context)
    }

    private fun loadSavedConfig(context: Context): RadioAlarmConfig? {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val streamUrl = preferences.getString("stream_url", null)?.takeIf { it.isNotBlank() }
            ?: return null
        val stationId = preferences.getString("station_id", null).orEmpty()
        val stationName = preferences.getString("station_name", null)?.takeIf { it.isNotBlank() }
            ?: "SONARA Radio"
        val hour = preferences.getInt("hour", 7).coerceIn(0, 23)
        val minute = preferences.getInt("minute", 0).coerceIn(0, 59)

        val station = RadioStation(
            id = stationId.ifBlank { streamUrl },
            name = stationName,
            streamUrl = streamUrl,
            favicon = preferences.getString("favicon", "").orEmpty(),
            country = preferences.getString("country", "").orEmpty(),
            countryCode = preferences.getString("country_code", "").orEmpty(),
            language = preferences.getString("language", "").orEmpty(),
            tags = preferences.getString("tags", "").orEmpty(),
            codec = preferences.getString("codec", "").orEmpty(),
            bitrate = preferences.getInt("bitrate", 0),
            homepage = preferences.getString("homepage", "").orEmpty()
        )
        return RadioAlarmConfig(
            hour = hour,
            minute = minute,
            station = station,
            repeatDaily = preferences.getBoolean("repeat_daily", true)
        )
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms(context)) return
        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun schedule(
        context: Context,
        hour: Int,
        minute: Int,
        station: RadioStation,
        repeatDaily: Boolean
    ): Boolean {
        if (!canScheduleExactAlarms(context)) return false

        val normalizedHour = hour.coerceIn(0, 23)
        val normalizedMinute = minute.coerceIn(0, 59)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("enabled", true)
            .putBoolean("repeat_daily", repeatDaily)
            .putInt("hour", normalizedHour)
            .putInt("minute", normalizedMinute)
            .putString("station_id", station.id)
            .putString("station_name", station.name)
            .putString("stream_url", station.streamUrl)
            .putString("favicon", station.favicon)
            .putString("country", station.country)
            .putString("country_code", station.countryCode)
            .putString("language", station.language)
            .putString("tags", station.tags)
            .putString("codec", station.codec)
            .putInt("bitrate", station.bitrate)
            .putString("homepage", station.homepage)
            .apply()

        val scheduled = scheduleNext(
            context,
            RadioAlarmConfig(normalizedHour, normalizedMinute, station, repeatDaily)
        )
        if (!scheduled) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("enabled", false)
                .apply()
        }
        return scheduled
    }

    /** Cancels the future alarm. It does not stop an alarm that is already ringing. */
    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmPendingIntent(context))
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("enabled", false)
            .apply()
    }

    /** A one-time alarm is consumed as soon as it fires; the radio itself keeps playing. */
    fun markOneTimeFired(context: Context) {
        cancel(context)
    }

    fun rescheduleIfEnabled(context: Context) {
        val config = load(context) ?: return
        if (canScheduleExactAlarms(context)) scheduleNext(context, config)
    }

    /** Schedules the next occurrence. Called again after a daily alarm fires. */
    fun scheduleNext(context: Context, config: RadioAlarmConfig): Boolean {
        if (!canScheduleExactAlarms(context)) return false

        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, config.hour)
            set(Calendar.MINUTE, config.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val showAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val showAppPendingIntent = PendingIntent.getActivity(
            context,
            SHOW_REQUEST_CODE,
            showAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return runCatching {
            context.getSystemService(AlarmManager::class.java).setAlarmClock(
                AlarmManager.AlarmClockInfo(next.timeInMillis, showAppPendingIntent),
                alarmPendingIntent(context)
            )
            true
        }.getOrDefault(false)
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, RadioAlarmReceiver::class.java)
            .setAction(ACTION_PLAY_ALARM)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
