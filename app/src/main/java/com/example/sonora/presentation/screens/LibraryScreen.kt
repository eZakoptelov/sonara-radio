package com.example.sonora.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.example.sonora.domain.model.RadioStation
import com.example.sonora.presentation.SonoraUiState
import com.example.sonora.presentation.components.StationRow
import com.example.sonora.presentation.localization.uiText
import com.example.sonora.presentation.theme.SonoraMuted
import com.example.sonora.presentation.theme.SonoraPrimary
import com.example.sonora.presentation.theme.SonoraText

@Composable
fun LibraryScreen(
    state: SonoraUiState,
    onStationClick: (RadioStation, List<RadioStation>) -> Unit,
    onFavoriteClick: (RadioStation) -> Unit,
    onClearHistory: () -> Unit
) {
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val stations = if (showHistory) state.history else state.favorites

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(uiText(state.radioLanguage, "library.title"), color = SonoraText, style = MaterialTheme.typography.headlineMedium)
        Text(uiText(state.radioLanguage, "library.subtitle"), color = SonoraMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 5.dp, bottom = 17.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !showHistory, onClick = { showHistory = false }, label = { Text(uiText(state.radioLanguage, "library.favorites")) })
            FilterChip(selected = showHistory, onClick = { showHistory = true }, label = { Text(uiText(state.radioLanguage, "library.history")) })
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (showHistory) uiText(state.radioLanguage, "library.recent") else uiText(state.radioLanguage, "library.favorite_stations"), color = SonoraText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (showHistory && state.history.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = SonoraMuted)
                    Text(uiText(state.radioLanguage, "library.clear"), color = SonoraMuted)
                }
            }
        }
        if (stations.isEmpty()) {
            Text(
                text = if (showHistory) uiText(state.radioLanguage, "library.empty_history") else uiText(state.radioLanguage, "library.empty_favorites"),
                color = SonoraMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                itemsIndexed(stations, key = { index, station -> if (showHistory) "${station.id}_$index" else station.id }) { _, station ->
                    StationRow(
                        station = station,
                        language = state.radioLanguage,
                        isFavorite = state.favorites.any { it.id == station.id },
                        onClick = { onStationClick(station, stations) },
                        onFavoriteClick = { onFavoriteClick(station) }
                    )
                }
            }
        }
    }
}
