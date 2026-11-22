package com.example.sonora.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.sonora.domain.model.RadioStation

@Composable
fun StationArtwork(
    station: RadioStation,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp
) {
    val context = LocalContext.current.applicationContext
    val artworkBrush = remember {
        Brush.linearGradient(
            listOf(Color(0xFF1A4563), Color(0xFF27316A), Color(0xFF39285E))
        )
    }
    val imageRequest = remember(station.favicon) {
        ImageRequest.Builder(context)
            .data(station.favicon)
            // Station tiles are small; decode a thumbnail instead of a full-size logo.
            .size(128, 128)
            .memoryCacheKey(station.favicon)
            .diskCacheKey(station.favicon)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(artworkBrush),
        contentAlignment = Alignment.Center
    ) {
        if (station.favicon.isNotBlank()) {
            AsyncImage(
                model = imageRequest,
                contentDescription = station.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = station.name.firstOrNull()?.uppercase() ?: "S",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Filled.Radio,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.16f),
                modifier = Modifier.fillMaxSize(0.42f)
            )
        }
    }
}
