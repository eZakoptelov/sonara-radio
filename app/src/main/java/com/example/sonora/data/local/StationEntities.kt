package com.example.sonora.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.sonora.domain.model.RadioStation

@Entity(tableName = "favorite_stations")
data class FavoriteStationEntity(
    @PrimaryKey val stationUuid: String,
    val name: String,
    val streamUrl: String,
    val favicon: String,
    val country: String,
    val countryCode: String,
    val language: String,
    val tags: String,
    val codec: String,
    val bitrate: Int,
    val homepage: String
)

@Entity(
    tableName = "play_history",
    indices = [Index(value = ["playedAt"])]
)
data class HistoryStationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stationUuid: String,
    val name: String,
    val streamUrl: String,
    val favicon: String,
    val country: String,
    val countryCode: String,
    val language: String,
    val tags: String,
    val codec: String,
    val bitrate: Int,
    val homepage: String,
    val playedAt: Long = System.currentTimeMillis()
)

fun RadioStation.toFavoriteEntity() = FavoriteStationEntity(
    stationUuid = id,
    name = name,
    streamUrl = streamUrl,
    favicon = favicon,
    country = country,
    countryCode = countryCode,
    language = language,
    tags = tags,
    codec = codec,
    bitrate = bitrate,
    homepage = homepage
)

fun FavoriteStationEntity.toDomain() = RadioStation(
    id = stationUuid,
    name = name,
    streamUrl = streamUrl,
    favicon = favicon,
    country = country,
    countryCode = countryCode,
    language = language,
    tags = tags,
    codec = codec,
    bitrate = bitrate,
    homepage = homepage
)

fun RadioStation.toHistoryEntity() = HistoryStationEntity(
    stationUuid = id,
    name = name,
    streamUrl = streamUrl,
    favicon = favicon,
    country = country,
    countryCode = countryCode,
    language = language,
    tags = tags,
    codec = codec,
    bitrate = bitrate,
    homepage = homepage
)

fun HistoryStationEntity.toDomain() = RadioStation(
    id = stationUuid,
    name = name,
    streamUrl = streamUrl,
    favicon = favicon,
    country = country,
    countryCode = countryCode,
    language = language,
    tags = tags,
    codec = codec,
    bitrate = bitrate,
    homepage = homepage
)
