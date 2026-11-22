package com.example.sonora.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sonora.domain.model.RadioStation
import com.example.sonora.presentation.SonoraUiState
import com.example.sonora.presentation.components.StationRow
import com.example.sonora.presentation.localization.uiText
import com.example.sonora.presentation.theme.SonoraMuted
import com.example.sonora.presentation.theme.SonoraPrimary
import com.example.sonora.presentation.theme.SonoraSurface
import com.example.sonora.presentation.theme.SonoraText

@Composable
fun SearchScreen(
    state: SonoraUiState,
    onQueryChange: (String) -> Unit,
    onStationClick: (RadioStation) -> Unit,
    onFavoriteClick: (RadioStation) -> Unit,
    onLoadMore: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(uiText(state.radioLanguage, "search.title"), color = SonoraText, style = MaterialTheme.typography.headlineMedium)
        Text(uiText(state.radioLanguage, "search.subtitle", state.radioLanguage?.title ?: "—"), color = SonoraMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 5.dp, bottom = 17.dp))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(17.dp),
            placeholder = { Text(uiText(state.radioLanguage, "search.placeholder"), color = SonoraMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = SonoraMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SonoraSurface,
                unfocusedContainerColor = SonoraSurface,
                focusedBorderColor = SonoraPrimary,
                unfocusedBorderColor = SonoraSurface
            )
        )

        if (state.isSearching) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = SonoraPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(uiText(state.radioLanguage, "search.searching"), color = SonoraMuted)
            }
        } else if (state.searchQuery.isBlank()) {
            Text(uiText(state.radioLanguage, "search.enter"), color = SonoraMuted, modifier = Modifier.padding(top = 28.dp))
        } else if (state.error != null) {
            Text(state.error, color = SonoraMuted, modifier = Modifier.padding(top = 28.dp))
        } else if (state.searchResults.isEmpty()) {
            Text(uiText(state.radioLanguage, "search.empty"), color = SonoraMuted, modifier = Modifier.padding(top = 28.dp))
        } else {
            Text(uiText(state.radioLanguage, "search.results", state.searchResults.size), color = SonoraMuted, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(state.searchResults, key = { it.id }) { station ->
                    StationRow(
                        station = station,
                        language = state.radioLanguage,
                        isFavorite = state.favorites.any { it.id == station.id },
                        onClick = { onStationClick(station) },
                        onFavoriteClick = { onFavoriteClick(station) }
                    )
                }
                if (state.isLoadingMoreSearch) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SonoraPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(uiText(state.radioLanguage, "search.loading_more"), color = SonoraMuted)
                        }
                    }
                } else if (state.searchLoadMoreError != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(state.searchLoadMoreError, color = SonoraMuted, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = onLoadMore) {
                                Text(uiText(state.radioLanguage, "common.retry_load"), color = SonoraPrimary)
                            }
                        }
                    }
                } else if (state.hasMoreSearchResults) {
                    item {
                        TextButton(
                            onClick = onLoadMore,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(uiText(state.radioLanguage, "search.load_more"), color = SonoraPrimary)
                        }
                    }
                }
            }
        }
    }
}
