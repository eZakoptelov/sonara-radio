package com.example.sonora

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.example.sonora.alarm.RadioAlarmScheduler
import okio.Path.Companion.toPath

class SonoraApplication : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        RadioAlarmScheduler.rescheduleIfEnabled(this)
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("sonara_station_artwork").absolutePath.toPath())
                    .maxSizeBytes(80L * 1024L * 1024L)
                    .build()
            }
            .build()
    }
}
