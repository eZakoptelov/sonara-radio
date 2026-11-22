package com.example.sonora.data.repository

import com.example.sonora.data.local.StationDao
import com.example.sonora.data.local.toDomain
import com.example.sonora.data.local.toFavoriteEntity
import com.example.sonora.data.local.toHistoryEntity
import com.example.sonora.data.remote.RadioBrowserApi
import com.example.sonora.domain.model.RadioLanguage
import com.example.sonora.domain.model.RadioStation
import com.example.sonora.domain.repository.RadioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RadioRepositoryImpl(
    private val api: RadioBrowserApi,
    private val dao: StationDao
) : RadioRepository {
    override suspend fun getPopularStations(
        language: RadioLanguage,
        limit: Int,
        offset: Int
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        api.popular(language, limit, offset)
    }

    override suspend fun getStationsByLanguage(
        language: RadioLanguage,
        limit: Int,
        offset: Int
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        api.byLanguage(language, limit, offset)
    }

    override suspend fun getStationsByCountryCode(
        countryCode: String,
        language: RadioLanguage,
        limit: Int,
        offset: Int
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        api.byCountryCode(countryCode, language, limit, offset)
    }

    override suspend fun searchStations(
        query: String,
        language: RadioLanguage,
        limit: Int,
        offset: Int
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        api.search(query, language, limit, offset)
    }

    override suspend fun getStationsByTag(
        tag: String,
        language: RadioLanguage,
        limit: Int,
        offset: Int
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        api.byTag(tag, language, limit, offset)
    }

    // Keep favourites and listening history across language changes.
    override fun observeFavorites(): Flow<List<RadioStation>> =
        dao.observeFavorites().map { stations -> stations.map { it.toDomain() } }

    override fun observeHistory(): Flow<List<RadioStation>> =
        dao.observeHistory().map { stations -> stations.map { it.toDomain() } }

    override suspend fun toggleFavorite(station: RadioStation) {
        if (dao.findFavorite(station.id) == null) {
            dao.addFavorite(station.toFavoriteEntity())
        } else {
            dao.removeFavorite(station.id)
        }
    }

    override suspend fun recordPlay(station: RadioStation) {
        dao.addToHistory(station.toHistoryEntity())
    }

    override suspend fun clearHistory() {
        dao.clearHistory()
    }
}
