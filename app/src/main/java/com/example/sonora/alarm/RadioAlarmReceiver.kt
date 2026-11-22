package com.example.sonora.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.sonora.player.RadioPlaybackService

class RadioAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val config = RadioAlarmScheduler.load(context) ?: return

        if (config.repeatDaily) {
            // Set up tomorrow's occurrence before starting the playback service.
            RadioAlarmScheduler.scheduleNext(context, config)
        } else {
            // A one-time alarm is disabled now, but its radio stream is not stopped.
            RadioAlarmScheduler.markOneTimeFired(context)
            context.sendBroadcast(
                Intent(RadioAlarmScheduler.ACTION_ALARM_CONFIG_CHANGED)
                    .setPackage(context.packageName)
            )
        }

        val playIntent = Intent(context, RadioPlaybackService::class.java).apply {
            action = RadioAlarmScheduler.ACTION_PLAY_ALARM
            putExtra(RadioAlarmScheduler.EXTRA_STREAM_URL, config.station.streamUrl)
            putExtra(RadioAlarmScheduler.EXTRA_STATION_NAME, config.station.name)
            putExtra(RadioAlarmScheduler.EXTRA_STATION_ARTWORK, config.station.favicon)
        }
        runCatching {
            ContextCompat.startForegroundService(context, playIntent)
        }
    }
}

class RadioAlarmBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                RadioAlarmScheduler.rescheduleIfEnabled(context)
            }
        }
    }
}
