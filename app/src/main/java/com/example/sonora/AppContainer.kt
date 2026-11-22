package com.example.sonora

import android.content.Context
import androidx.room.Room
import com.example.sonora.data.local.SonoraDatabase
import com.example.sonora.data.remote.RadioBrowserApi
import com.example.sonora.data.repository.RadioRepositoryImpl
import com.example.sonora.data.settings.SettingsRepository
import com.example.sonora.domain.repository.RadioRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        SonoraDatabase::class.java,
        "sonora_database"
    ).build()

    val settingsRepository = SettingsRepository(context)
    val radioRepository: RadioRepository = RadioRepositoryImpl(
        api = RadioBrowserApi(),
        dao = database.stationDao()
    )
}
