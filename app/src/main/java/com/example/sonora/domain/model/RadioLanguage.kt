package com.example.sonora.domain.model

import java.util.Locale

/**
 * Preferred language of stations shown in the catalogue.
 * apiValue is the Radio Browser language slug; aliases are used to verify station metadata.
 */
enum class RadioLanguage(
    val code: String,
    val title: String,
    val nativeTitle: String,
    val flag: String,
    val apiValue: String,
    private val aliases: Set<String>
) {
    RUSSIAN("ru", "Русский", "Русский", "🇷🇺", "russian", setOf("russian", "русский", "rus", "ru")),
    ENGLISH("en", "English", "English", "🇬🇧", "english", setOf("english", "английский", "eng", "en")),
    CHINESE("zh", "中文", "中文", "🇨🇳", "chinese", setOf("chinese", "中文", "китайский", "mandarin", "zho", "chi", "zh")),
    TURKISH("tr", "Türkçe", "Türkçe", "🇹🇷", "turkish", setOf("turkish", "türkçe", "турецкий", "tur", "tr")),
    ITALIAN("it", "Italiano", "Italiano", "🇮🇹", "italian", setOf("italian", "italiano", "итальянский", "ita", "it")),
    BELARUSIAN("be", "Беларуская", "Беларуская", "🇧🇾", "belarusian", setOf("belarusian", "беларуская", "беларуский", "bel", "be"));
    fun matchesStationLanguage(value: String): Boolean {
        val normalized = value.lowercase(Locale.ROOT)
            .replace('_', ' ')
            .replace('-', ' ')
        if (normalized.isBlank()) return false
        return aliases.any { alias ->
            val normalizedAlias = alias.lowercase(Locale.ROOT)
            if (normalizedAlias.length <= 3 && normalizedAlias.all { it in 'a'..'z' }) {
                Regex("(^|[,;|\\s])${Regex.escape(normalizedAlias)}($|[,;|\\s])")
                    .containsMatchIn(normalized)
            } else {
                normalized.contains(normalizedAlias)
            }
        }
    }

    companion object {
        fun fromName(value: String?): RadioLanguage? =
            entries.firstOrNull { it.name == value || it.code == value }

        fun fromApiValue(value: String): RadioLanguage? =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
    }
}
