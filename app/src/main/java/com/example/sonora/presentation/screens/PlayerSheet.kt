package com.example.sonora.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.sonora.data.settings.AppTheme
import com.example.sonora.domain.model.RadioLanguage
import com.example.sonora.domain.model.RadioStation
import com.example.sonora.presentation.components.StationArtwork
import com.example.sonora.presentation.localization.uiText
import com.example.sonora.presentation.theme.SonoraBackground
import com.example.sonora.presentation.theme.SonoraGreen
import com.example.sonora.presentation.theme.SonoraMuted
import com.example.sonora.presentation.theme.SonoraPrimary
import com.example.sonora.presentation.theme.SonoraSecondary
import com.example.sonora.presentation.theme.SonoraSurface
import com.example.sonora.presentation.theme.SonoraText
import com.example.sonora.presentation.theme.SonoraTheme

@Composable
fun PlayerSheet(
    station: RadioStation,
    language: RadioLanguage?,
    trackTitle: String?,
    trackArtist: String?,
    trackArtworkUri: String?,
    isFavorite: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    error: String?,
    queuePosition: Int,
    queueSize: Int,
    onTogglePlayback: () -> Unit,
    onPreviousStation: () -> Unit,
    onNextStation: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit
) {
    val actualTrackTitle = trackTitle
        ?.trim()
        ?.takeIf { it.isNotBlank() && !it.equals(station.name, ignoreCase = true) }
    val actualTrackArtist = trackArtist
        ?.trim()
        ?.takeIf {
            it.isNotBlank() &&
                !it.equals(station.subtitle, ignoreCase = true) &&
                !it.startsWith("SONARA", ignoreCase = true)
        }
    val hasTrackMetadata = actualTrackTitle != null
    val displayedTitle = actualTrackTitle ?: station.name
    val displayedArtist = actualTrackArtist ?: if (hasTrackMetadata) {
        uiText(language, "player.artist_unavailable")
    } else {
        uiText(language, "player.track_metadata_unavailable")
    }
    val artworkModel = trackArtworkUri?.takeIf { it.isNotBlank() }
        ?: station.favicon.takeIf { it.isNotBlank() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 760.dp)
    ) {
        // Scale the cover and gaps to the sheet's available height. The player has
        // no verticalScroll: all essential controls remain visible on one screen.
        val compact = maxHeight < 680.dp
        val artworkSize = (maxHeight * 0.28f).coerceIn(136.dp, 220.dp)
        val sectionGap = if (compact) 8.dp else 11.dp
        val verticalPadding = if (compact) 8.dp else 11.dp
        val visualizerHeight = if (compact) 28.dp else 38.dp
        val playButtonSize = if (compact) 72.dp else 80.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = verticalPadding)
                .pointerInput(station.id, queuePosition, queueSize) {
                    var horizontalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            horizontalDrag += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            when {
                                horizontalDrag < -72f -> onNextStation()
                                horizontalDrag > 72f -> onPreviousStation()
                            }
                            horizontalDrag = 0f
                        },
                        onDragCancel = { horizontalDrag = 0f }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(sectionGap)
        ) {
            // Compact header: branding, live state and close action share one row.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        uiText(language, "player.brand"),
                        color = SonoraText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp
                    )
                    Text(
                        uiText(language, "player.tagline"),
                        color = SonoraMuted,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.1.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SonoraGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(SonoraGreen))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "LIVE",
                        color = SonoraGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(42.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = uiText(language, "player.collapse"),
                        tint = SonoraMuted,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            // Large cover remains the visual focus. Favourite is now an overlay,
            // not a separate row that pushes the playback controls down.
            Box(
                modifier = Modifier
                    .size(artworkSize)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                SonoraPrimary.copy(alpha = 0.25f),
                                SonoraSecondary.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(artworkSize - 14.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    SonoraSurface,
                                    SonoraPrimary.copy(alpha = 0.22f),
                                    SonoraSecondary.copy(alpha = 0.18f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = artworkModel,
                        transitionSpec = {
                            fadeIn(tween(320)) togetherWith fadeOut(tween(220))
                        },
                        label = "sonara-cover"
                    ) { model ->
                        if (model != null) {
                            AsyncImage(
                                model = model,
                                contentDescription = uiText(language, "player.artwork"),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            StationArtwork(
                                station = station,
                                modifier = Modifier.fillMaxSize(),
                                cornerRadius = 26.dp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SonoraBackground.copy(alpha = 0.94f))
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = uiText(language, "player.favorite"),
                        tint = if (isFavorite) SonoraPrimary else SonoraText,
                        modifier = Modifier.size(29.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    if (hasTrackMetadata) uiText(language, "player.now_playing")
                    else uiText(language, "player.station"),
                    color = SonoraSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
                AnimatedContent(
                    targetState = displayedTitle to displayedArtist,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                    },
                    label = "sonara-track-info"
                ) { track ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            track.first,
                            color = SonoraText,
                            style = if (compact) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = if (compact) 25.sp else 29.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (actualTrackArtist != null) {
                                uiText(language, "player.artist_line", actualTrackArtist)
                            } else {
                                track.second
                            },
                            color = if (hasTrackMetadata || actualTrackArtist != null) SonoraSecondary else SonoraMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (hasTrackMetadata || actualTrackArtist != null) FontWeight.Medium else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50.dp))
                    .background(SonoraSurface.copy(alpha = 0.72f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.Wifi,
                    contentDescription = null,
                    tint = SonoraSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(7.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        station.name.ifBlank { uiText(language, "mini.radio") },
                        color = SonoraText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        buildString {
                            append(station.country.ifBlank { uiText(language, "player.direct") })
                            if (station.codec.isNotBlank()) append("  ·  ${station.codec.uppercase()}")
                            if (station.bitrate > 0) {
                                append("  ·  ${uiText(language, "player.bitrate", station.bitrate)}")
                            }
                        },
                        color = SonoraMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (error != null) {
                Text(
                    error,
                    color = Color(0xFFFF8C8C),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LiveWaveform(
                isPlaying = isPlaying && !isBuffering,
                height = visualizerHeight
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousStation,
                    enabled = queueSize > 1,
                    modifier = Modifier.size(58.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = uiText(language, "player.previous"),
                        tint = if (queueSize > 1) SonoraText else SonoraMuted.copy(alpha = 0.35f),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(Modifier.width(20.dp))
                Box(
                    modifier = Modifier
                        .size(playButtonSize)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(SonoraPrimary, SonoraSecondary))),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onTogglePlayback, modifier = Modifier.size(playButtonSize)) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                color = Color(0xFF071321),
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) uiText(language, "player.pause")
                                else uiText(language, "player.play"),
                                tint = Color(0xFF071321),
                                modifier = Modifier.size(43.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(20.dp))

                IconButton(
                    onClick = onNextStation,
                    enabled = queueSize > 1,
                    modifier = Modifier.size(58.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = uiText(language, "player.next"),
                        tint = if (queueSize > 1) SonoraText else SonoraMuted.copy(alpha = 0.35f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            if (queueSize > 1) {
                Text(
                    uiText(language, "player.queue", queuePosition, queueSize),
                    color = SonoraMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LiveWaveform(isPlaying: Boolean, height: androidx.compose.ui.unit.Dp) {
    val transition = rememberInfiniteTransition(label = "sonara-spectrum")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(32) { index ->
            val target = (10 + ((index * 7) % 10) * 3).toFloat()
            val animatedHeight by transition.animateFloat(
                initialValue = 5f,
                targetValue = target,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 340 + (index % 7) * 55,
                        delayMillis = (index % 6) * 40
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "sonara-spectrum-$index"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.5.dp)
                    .width(4.dp)
                    .height((if (isPlaying) animatedHeight else 4f).dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(SonoraSecondary, SonoraPrimary)))
            )
        }
    }
}

// Android Studio Compose Preview: player layout without starting an audio stream.
private val previewStation = RadioStation(
    id = "sonara-preview-station",
    name = "Europa Plus",
    streamUrl = "https://example.com/stream",
    country = "Russia",
    countryCode = "RU",
    language = "russian",
    tags = "pop, hits",
    codec = "MP3",
    bitrate = 128,
    homepage = "https://europaplus.ru"
)

@Composable
private fun PlayerSheetPreviewContent(language: RadioLanguage) {
    SonoraTheme(themeMode = AppTheme.NEON) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SonoraBackground
        ) {
            PlayerSheet(
                station = previewStation,
                language = language,
                trackTitle = "Midnight City",
                trackArtist = "M83",
                trackArtworkUri = null,
                isFavorite = true,
                isPlaying = true,
                isBuffering = false,
                error = null,
                queuePosition = 3,
                queueSize = 24,
                onTogglePlayback = {},
                onPreviousStation = {},
                onNextStation = {},
                onToggleFavorite = {},
                onClose = {}
            )
        }
    }
}

@Preview(
    name = "SONARA Player · Русский",
    showBackground = true,
    backgroundColor = 0xFF080A14,
    widthDp = 393,
    heightDp = 800
)
@Composable
fun SonaraPlayerRussianPreview() {
    PlayerSheetPreviewContent(language = RadioLanguage.RUSSIAN)
}

@Preview(
    name = "SONARA Player · English",
    showBackground = true,
    backgroundColor = 0xFF080A14,
    widthDp = 393,
    heightDp = 800
)
@Composable
fun SonaraPlayerEnglishPreview() {
    PlayerSheetPreviewContent(language = RadioLanguage.ENGLISH)
}

@Preview(
    name = "SONARA Player · Compact",
    showBackground = true,
    backgroundColor = 0xFF080A14,
    widthDp = 360,
    heightDp = 640
)
@Composable
fun SonaraPlayerCompactPreview() {
    PlayerSheetPreviewContent(language = RadioLanguage.RUSSIAN)
}
