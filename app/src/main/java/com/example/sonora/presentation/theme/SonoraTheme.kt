package com.example.sonora.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.sonora.data.settings.AppTheme

@Composable
fun SonoraTheme(
    themeMode: AppTheme = AppTheme.NEON,
    content: @Composable () -> Unit
) {
    val palette = paletteFor(themeMode)
    val colorScheme = if (themeMode == AppTheme.LIGHT) {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            secondary = palette.secondary,
            onSecondary = Color.White,
            tertiary = palette.secondary,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.muted,
            outline = palette.divider,
            error = palette.error
        )
    } else {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = Color(0xFF100A1D),
            secondary = palette.secondary,
            onSecondary = Color(0xFF06131B),
            tertiary = palette.secondary,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.muted,
            outline = palette.divider,
            error = palette.error
        )
    }

    CompositionLocalProvider(LocalSonoraPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
