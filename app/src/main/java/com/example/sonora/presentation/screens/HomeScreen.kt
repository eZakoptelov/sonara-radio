package com.example.sonora.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sonora.R
import com.example.sonora.domain.model.RadioStation
import com.example.sonora.presentation.SonoraUiState
import com.example.sonora.presentation.components.StationRow
import com.example.sonora.presentation.localization.uiText
import com.example.sonora.presentation.theme.SonoraMuted
import com.example.sonora.presentation.theme.SonoraPrimary
import com.example.sonora.presentation.theme.SonoraSecondary
import com.example.sonora.presentation.theme.SonoraSurface
import com.example.sonora.presentation.theme.SonoraSurfaceElevated
import com.example.sonora.presentation.theme.SonoraText

private data class HomeFilter(val key: String, val label: String)

private fun genres(language: com.example.sonora.domain.model.RadioLanguage?) = listOf(
    HomeFilter("all", uiText(language, "filter.all")),
    HomeFilter("tag:pop", uiText(language, "filter.pop")),
    HomeFilter("tag:rock", uiText(language, "filter.rock")),
    HomeFilter("tag:jazz", uiText(language, "filter.jazz")),
    HomeFilter("tag:electronic", uiText(language, "filter.electronic")),
    HomeFilter("tag:news", uiText(language, "filter.news")),
    HomeFilter("tag:classical", uiText(language, "filter.classical"))
)

private fun filterTitle(
    filter: String?,
    language: com.example.sonora.domain.model.RadioLanguage?
): String = when (filter) {
    "tag:pop" -> uiText(language, "filter_title.pop")
    "tag:rock" -> uiText(language, "filter_title.rock")
    "tag:jazz" -> uiText(language, "filter_title.jazz")
    "tag:electronic" -> uiText(language, "filter_title.electronic")
    "tag:news" -> uiText(language, "filter_title.news")
    "tag:classical" -> uiText(language, "filter_title.classical")
    else -> uiText(language, "filter_title.popular")
}

@Composable
fun HomeScreen(
    state: SonoraUiState,
    onSearchClick: () -> Unit,
    onStationClick: (RadioStation) -> Unit,
    onFavoriteClick: (RadioStation) -> Unit,
    onGenreClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    val genreFilters = remember(state.radioLanguage) { genres(state.radioLanguage) }
    val favoriteIds = remember(state.favorites) {
        state.favorites.asSequence().map { it.id }.toHashSet()
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.sonara_brand_mark),
                    contentDescription = "SONARA Online Radio",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(15.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(11.dp))
                Text(
                    "SONARA",
                    color = SonoraText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.65.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SonoraSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = "SONARA", tint = SonoraPrimary)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(uiText(state.radioLanguage, "home.headline"), style = MaterialTheme.typography.headlineMedium, color = SonoraText, fontWeight = FontWeight.Bold)
                Text(uiText(state.radioLanguage, "home.intro"), color = SonoraMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(17.dp))
                    .background(SonoraSurface)
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = SonoraMuted)
                Spacer(Modifier.width(10.dp))
                Text(uiText(state.radioLanguage, "home.search"), color = SonoraMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(25.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(SonoraPrimary.copy(alpha = 0.22f), SonoraSecondary.copy(alpha = 0.15f), SonoraSurfaceElevated)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.78f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(uiText(state.radioLanguage, "home.banner_eyebrow"), color = SonoraSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    Text(uiText(state.radioLanguage, "home.banner_title"), color = SonoraText, fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold)
                    Text(uiText(state.radioLanguage, "home.banner_subtitle"), color = SonoraText.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = SonoraText.copy(alpha = 0.17f),
                    modifier = Modifier.align(Alignment.CenterEnd).size(88.dp)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                SectionTitle(title = uiText(state.radioLanguage, "home.catalog"), subtitle = uiText(state.radioLanguage, "home.catalog_subtitle"))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(genreFilters, key = { it.key }, contentType = { "genre" }) { filter ->
                        FilterChip(
                            selected = (state.selectedGenre ?: "all") == filter.key,
                            onClick = { onGenreClick(filter.key) },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = filterTitle(state.selectedGenre, state.radioLanguage),
                        color = SonoraText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(uiText(state.radioLanguage, "home.loaded", state.popularStations.size), color = SonoraMuted, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = SonoraPrimary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(uiText(state.radioLanguage, "home.refresh"), color = SonoraPrimary)
                }
            }
        }

        if (state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SonoraPrimary, strokeWidth = 2.dp)
                }
            }
        }

        if (state.error != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SonoraSurface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(uiText(state.radioLanguage, "home.load_error"), color = SonoraText, fontWeight = FontWeight.SemiBold)
                    Text(state.error, color = SonoraMuted, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onRefresh) { Text(uiText(state.radioLanguage, "common.retry"), color = SonoraPrimary) }
                }
            }
        }

        if (!state.isLoading && state.error == null && state.popularStations.isEmpty()) {
            item {
                Text(uiText(state.radioLanguage, "home.empty"), color = SonoraMuted, modifier = Modifier.padding(vertical = 18.dp))
            }
        }

        items(
            items = state.popularStations,
            key = { station -> station.id },
            contentType = { "station" }
        ) { station ->
            StationRow(
                station = station,
                language = state.radioLanguage,
                isFavorite = station.id in favoriteIds,
                onClick = { onStationClick(station) },
                onFavoriteClick = { onFavoriteClick(station) }
            )
        }

        if (state.isLoadingMore) {
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
                    Text(uiText(state.radioLanguage, "home.loading_more"), color = SonoraMuted)
                }
            }
        } else if (state.loadMoreError != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SonoraSurface).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(uiText(state.radioLanguage, "home.next_error"), color = SonoraText, fontWeight = FontWeight.SemiBold)
                    Text(state.loadMoreError, color = SonoraMuted, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onLoadMore) {
                        Text(uiText(state.radioLanguage, "common.retry_load"), color = SonoraPrimary)
                    }
                }
            }
        } else if (state.hasMoreStations) {
            item {
                TextButton(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 12.dp)
                ) {
                    Text(uiText(state.radioLanguage, "home.load_more"), color = SonoraPrimary)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = SonoraText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = SonoraMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
