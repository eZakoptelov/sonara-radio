package com.example.sonora.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.sonora.data.settings.AppTheme

data class SonoraPalette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primary: Color,
    val secondary: Color,
    val text: Color,
    val muted: Color,
    val divider: Color,
    val green: Color,
    val error: Color
)

private val NeonPalette = SonoraPalette(
    background = Color(0xFF090914),
    surface = Color(0xFF131326),
    surfaceElevated = Color(0xFF1C1B36),
    primary = Color(0xFFB66BFF),
    secondary = Color(0xFF42D8FF),
    text = Color(0xFFF7F4FF),
    muted = Color(0xFFA29DBB),
    divider = Color(0xFF302B4A),
    green = Color(0xFF63DDAA),
    error = Color(0xFFFF858F)
)

private val OceanPalette = SonoraPalette(
    background = Color(0xFF07131D),
    surface = Color(0xFF0D2432),
    surfaceElevated = Color(0xFF143547),
    primary = Color(0xFF38BDF8),
    secondary = Color(0xFF5EEAD4),
    text = Color(0xFFEAFBFF),
    muted = Color(0xFF93B4C2),
    divider = Color(0xFF214354),
    green = Color(0xFF5DE4B5),
    error = Color(0xFFFF8C9B)
)

private val AmoledPalette = SonoraPalette(
    background = Color(0xFF000000),
    surface = Color(0xFF0D0D0F),
    surfaceElevated = Color(0xFF19191D),
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF03DAC6),
    text = Color(0xFFFFFFFF),
    muted = Color(0xFFA6A6B0),
    divider = Color(0xFF26262B),
    green = Color(0xFF67E6A7),
    error = Color(0xFFFF6B6B)
)

private val LightPalette = SonoraPalette(
    background = Color(0xFFF4F5FA),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFE9EAF4),
    primary = Color(0xFF6C42C1),
    secondary = Color(0xFF007E9D),
    text = Color(0xFF171421),
    muted = Color(0xFF625F70),
    divider = Color(0xFFD9D7E3),
    green = Color(0xFF17895D),
    error = Color(0xFFB3261E)
)

private val SunsetPalette = SonoraPalette(
    background = Color(0xFF160D14),
    surface = Color(0xFF26131F),
    surfaceElevated = Color(0xFF39202B),
    primary = Color(0xFFFF8A5B),
    secondary = Color(0xFFFFC857),
    text = Color(0xFFFFF3EB),
    muted = Color(0xFFC7A7A0),
    divider = Color(0xFF55333C),
    green = Color(0xFF72D6A1),
    error = Color(0xFFFF647C)
)

private val ForestPalette = SonoraPalette(
    background = Color(0xFF07130F),
    surface = Color(0xFF0D2119),
    surfaceElevated = Color(0xFF173327),
    primary = Color(0xFF54D68B),
    secondary = Color(0xFFB7E36B),
    text = Color(0xFFEDFFF4),
    muted = Color(0xFF94B6A3),
    divider = Color(0xFF264536),
    green = Color(0xFF70E0A0),
    error = Color(0xFFFF7D84)
)

private val RosePalette = SonoraPalette(
    background = Color(0xFF160C18),
    surface = Color(0xFF251329),
    surfaceElevated = Color(0xFF38203D),
    primary = Color(0xFFFF70B7),
    secondary = Color(0xFFC69CFF),
    text = Color(0xFFFFF2FB),
    muted = Color(0xFFC2A9BE),
    divider = Color(0xFF4C3151),
    green = Color(0xFF78D9B0),
    error = Color(0xFFFF7B8B)
)

private val GraphitePalette = SonoraPalette(
    background = Color(0xFF101318),
    surface = Color(0xFF1A2028),
    surfaceElevated = Color(0xFF252D38),
    primary = Color(0xFFA7B8D8),
    secondary = Color(0xFF63C5D4),
    text = Color(0xFFF2F5FA),
    muted = Color(0xFFA3AFBD),
    divider = Color(0xFF333E4A),
    green = Color(0xFF70D6A5),
    error = Color(0xFFFF7777)
)

fun paletteFor(theme: AppTheme): SonoraPalette = when (theme) {
    AppTheme.NEON -> NeonPalette
    AppTheme.OCEAN -> OceanPalette
    AppTheme.AMOLED -> AmoledPalette
    AppTheme.LIGHT -> LightPalette
    AppTheme.SUNSET -> SunsetPalette
    AppTheme.FOREST -> ForestPalette
    AppTheme.ROSE -> RosePalette
    AppTheme.GRAPHITE -> GraphitePalette
}

val LocalSonoraPalette = staticCompositionLocalOf { NeonPalette }

val SonoraBackground: Color
    @Composable get() = LocalSonoraPalette.current.background
val SonoraSurface: Color
    @Composable get() = LocalSonoraPalette.current.surface
val SonoraSurfaceElevated: Color
    @Composable get() = LocalSonoraPalette.current.surfaceElevated
val SonoraPrimary: Color
    @Composable get() = LocalSonoraPalette.current.primary
val SonoraSecondary: Color
    @Composable get() = LocalSonoraPalette.current.secondary
val SonoraText: Color
    @Composable get() = LocalSonoraPalette.current.text
val SonoraMuted: Color
    @Composable get() = LocalSonoraPalette.current.muted
val SonoraDivider: Color
    @Composable get() = LocalSonoraPalette.current.divider
val SonoraGreen: Color
    @Composable get() = LocalSonoraPalette.current.green
val SonoraError: Color
    @Composable get() = LocalSonoraPalette.current.error
