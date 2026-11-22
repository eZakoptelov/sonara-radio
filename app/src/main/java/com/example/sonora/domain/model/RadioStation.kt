package com.example.sonora.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class RadioStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val favicon: String = "",
    val country: String = "",
    val countryCode: String = "",
    val language: String = "",
    val tags: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val homepage: String = ""
) {
    val subtitle: String
        get() = listOf(country, tags.split(",").firstOrNull()?.trim().orEmpty())
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" • ")
            .ifBlank { "Интернет-радио" }
}
