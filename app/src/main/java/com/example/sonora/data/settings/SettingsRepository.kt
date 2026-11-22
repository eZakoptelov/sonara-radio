package com.example.sonora.data.settings

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.sonora.domain.model.RadioLanguage
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.sonoraDataStore by preferencesDataStore(name = "sonora_settings")

private val BUFFER_MODE_KEY = stringPreferencesKey("buffer_mode")
private val APP_THEME_KEY = stringPreferencesKey("app_theme")
private val RADIO_LANGUAGE_KEY = stringPreferencesKey("radio_language")

enum class AppTheme(
    val title: String,
    val description: String
) {
    NEON("Неон", "Фирменная фиолетово-голубая тема"),
    OCEAN("Океан", "Холодные синие и бирюзовые оттенки"),
    AMOLED("AMOLED", "Чёрный фон и яркие акценты"),
    LIGHT("Светлая", "Светлый интерфейс на каждый день"),
    SUNSET("Закат", "Тёплые оранжевые и янтарные оттенки"),
    FOREST("Лес", "Глубокие зелёные и природные оттенки"),
    ROSE("Розовый кварц", "Мягкие розовые и лиловые оттенки"),
    GRAPHITE("Графит", "Сдержанные серые и холодные голубые оттенки");

    companion object {
        fun fromName(value: String?): AppTheme =
            entries.firstOrNull { it.name == value } ?: NEON
    }
}

data class EqualizerSettings(
    val enabled: Boolean = true,
    /** Five bands in dB: 60, 230, 910, 3600 and 14000 Hz. */
    val bands: List<Float> = List(5) { 0f }
) {
    fun normalized(): EqualizerSettings = copy(
        bands = List(5) { index -> bands.getOrElse(index) { 0f }.coerceIn(-12f, 12f) }
    )
}

/** Buffering, appearance and audio-effect preferences. */
enum class BufferMode(
    val title: String,
    val description: String,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val playbackBufferMs: Int,
    val rebufferMs: Int
) {
    AUTO("Автоматическое качество", "Подстраивает доступное качество и буфер под сеть", 15_000, 90_000, 1_500, 8_000),
    LOW_LATENCY("Быстрый старт", "Меньше задержка, выше требования к сети", 8_000, 20_000, 800, 2_000),
    BALANCED("Баланс", "Оптимальный вариант на каждый день", 15_000, 45_000, 1_500, 4_000),
    STABLE("Стабильное вещание", "Больше буфер, меньше риск прерываний", 30_000, 90_000, 2_500, 6_000);

    companion object {
        fun fromName(value: String?): BufferMode =
            entries.firstOrNull { it.name == value } ?: AUTO
    }
}

object EqualizerContract {
    const val ACTION_UPDATE = "com.example.sonora.action.UPDATE_EQUALIZER"
    const val PREFERENCES = "radio_zhenya_audio"
    const val ENABLED_KEY = "equalizer_enabled"
    const val BAND_KEY_PREFIX = "equalizer_band_"
}

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext

    val bufferMode: Flow<BufferMode> = appContext.sonoraDataStore.data.map { preferences ->
        BufferMode.fromName(preferences[BUFFER_MODE_KEY])
    }

    val appTheme: Flow<AppTheme> = appContext.sonoraDataStore.data.map { preferences ->
        AppTheme.fromName(preferences[APP_THEME_KEY])
    }

    /** Null means the user has not completed the first-launch language selection yet. */
    val radioLanguage: Flow<RadioLanguage?> = appContext.sonoraDataStore.data.map { preferences ->
        RadioLanguage.fromName(preferences[RADIO_LANGUAGE_KEY])
    }

    suspend fun setRadioLanguage(language: RadioLanguage) {
        appContext.sonoraDataStore.edit { preferences ->
            preferences[RADIO_LANGUAGE_KEY] = language.name
        }
        appContext.getSharedPreferences("sonora_player", Context.MODE_PRIVATE)
            .edit()
            .putString("radio_language", language.code)
            .apply()
    }

    suspend fun setBufferMode(mode: BufferMode) {
        appContext.sonoraDataStore.edit { preferences ->
            preferences[BUFFER_MODE_KEY] = mode.name
        }
        // SharedPreferences mirror lets the playback service read the setting synchronously.
        appContext.getSharedPreferences("sonora_player", Context.MODE_PRIVATE)
            .edit()
            .putString("buffer_mode", mode.name)
            .apply()
    }

    suspend fun setAppTheme(theme: AppTheme) {
        appContext.sonoraDataStore.edit { preferences ->
            preferences[APP_THEME_KEY] = theme.name
        }
    }

    fun getEqualizerSettings(): EqualizerSettings {
        val preferences = appContext.getSharedPreferences(
            EqualizerContract.PREFERENCES,
            Context.MODE_PRIVATE
        )
        return EqualizerSettings(
            enabled = preferences.getBoolean(EqualizerContract.ENABLED_KEY, true),
            bands = List(5) { index ->
                preferences.getFloat("${EqualizerContract.BAND_KEY_PREFIX}$index", 0f)
            }
        ).normalized()
    }

    fun setEqualizerSettings(settings: EqualizerSettings) {
        val normalized = settings.normalized()
        val editor = appContext.getSharedPreferences(
            EqualizerContract.PREFERENCES,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(EqualizerContract.ENABLED_KEY, normalized.enabled)

        normalized.bands.forEachIndexed { index, level ->
            editor.putFloat("${EqualizerContract.BAND_KEY_PREFIX}$index", level)
        }
        editor.apply()

        // The playback service owns the actual Android Equalizer effect and applies changes live.
        appContext.sendBroadcast(
            Intent(EqualizerContract.ACTION_UPDATE).setPackage(appContext.packageName)
        )
    }

    fun getBufferModeForPlayer(): BufferMode {
        val stored = appContext.getSharedPreferences("sonora_player", Context.MODE_PRIVATE)
            .getString("buffer_mode", BufferMode.AUTO.name)
        return BufferMode.fromName(stored)
    }
}
