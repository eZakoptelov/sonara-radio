package com.example.sonora.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Query("SELECT * FROM favorite_stations ORDER BY name COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<FavoriteStationEntity>>

    @Query("SELECT * FROM favorite_stations WHERE stationUuid = :stationId LIMIT 1")
    suspend fun findFavorite(stationId: String): FavoriteStationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(station: FavoriteStationEntity)

    @Query("DELETE FROM favorite_stations WHERE stationUuid = :stationId")
    suspend fun removeFavorite(stationId: String)

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT 50")
    fun observeHistory(): Flow<List<HistoryStationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToHistory(station: HistoryStationEntity)

    @Query("DELETE FROM play_history")
    suspend fun clearHistory()
}
