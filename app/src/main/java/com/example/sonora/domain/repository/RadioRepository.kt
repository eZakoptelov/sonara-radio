package com.example.sonora.domain.repository

import com.example.sonora.domain.model.RadioLanguage
import com.example.sonora.domain.model.RadioStation
import kotlinx.coroutines.flow.Flow

interface RadioRepository {
    suspend fun getPopularStations(
        language: RadioLanguage,
        limit: Int = 10,
        offset: Int = 0
    ): List<RadioStation>

    suspend fun getStationsByLanguage(
        language: RadioLanguage,
        limit: Int = 10,
        offset: Int = 0
    ): List<RadioStation>

    suspend fun getStationsByCountryCode(
        countryCode: String,
        language: RadioLanguage,
        limit: Int = 10,
        offset: Int = 0
    ): List<RadioStation>

    suspend fun searchStations(
        query: String,
        language: RadioLanguage,
        limit: Int = 10,
        offset: Int = 0
    ): List<RadioStation>

    suspend fun getStationsByTag(
        tag: String,
        language: RadioLanguage,
        limit: Int = 10,
        offset: Int = 0
    ): List<RadioStation>

    fun observeFavorites(): Flow<List<RadioStation>>
    fun observeHistory(): Flow<List<RadioStation>>
    suspend fun toggleFavorite(station: RadioStation)
    suspend fun recordPlay(station: RadioStation)
    suspend fun clearHistory()
}
