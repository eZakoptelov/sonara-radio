package com.example.sonora.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sonora.data.settings.EqualizerSettings
import com.example.sonora.domain.model.RadioLanguage
import com.example.sonora.presentation.localization.uiText
import com.example.sonora.presentation.theme.SonoraMuted
import com.example.sonora.presentation.theme.SonoraPrimary
import com.example.sonora.presentation.theme.SonoraSurface
import com.example.sonora.presentation.theme.SonoraText

private data class EqualizerBand(val label: String, val frequency: String)

private fun equalizerBands(language: RadioLanguage?) = listOf(
    EqualizerBand(uiText(language, "eq.low_bass"), "60 Hz"),
    EqualizerBand(uiText(language, "eq.bass"), "230 Hz"),
    EqualizerBand(uiText(language, "eq.mid"), "910 Hz"),
    EqualizerBand(uiText(language, "eq.upper_mid"), "3.6 kHz"),
    EqualizerBand(uiText(language, "eq.high"), "14 kHz")
)

@Composable
fun EqualizerScreen(
    settings: EqualizerSettings,
    onSettingsChange: (EqualizerSettings) -> Unit,
    onBack: () -> Unit,
    language: RadioLanguage?
) {
    var localSettings by remember(settings) { mutableStateOf(settings.normalized()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = uiText(language, "eq.back"), tint = SonoraText)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(uiText(language, "eq.title"), color = SonoraText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(uiText(language, "eq.subtitle"), color = SonoraMuted, style = MaterialTheme.typography.bodySmall)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SonoraSurface, RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = SonoraPrimary)
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(uiText(language, "eq.effect"), color = SonoraText, fontWeight = FontWeight.SemiBold)
                    Text(uiText(language, "eq.effect_description"), color = SonoraMuted, style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = localSettings.enabled,
                    onCheckedChange = { enabled ->
                        val updated = localSettings.copy(enabled = enabled)
                        localSettings = updated
                        onSettingsChange(updated)
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SonoraSurface, RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(uiText(language, "eq.bands"), color = SonoraMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = {
                        val reset = localSettings.copy(bands = List(5) { 0f })
                        localSettings = reset
                        onSettingsChange(reset)
                    }
                ) {
                    Text(uiText(language, "eq.reset"), color = SonoraPrimary)
                }
            }

            equalizerBands(language).forEachIndexed { index, band ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(band.label, color = SonoraText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(band.frequency, color = SonoraMuted, style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.size(12.dp))
                        Text(
                            "${localSettings.bands[index].toInt().let { if (it > 0) "+$it" else it.toString() }} dB",
                            color = SonoraPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = localSettings.bands[index],
                        onValueChange = { value ->
                            val updatedBands = localSettings.bands.toMutableList().apply {
                                this[index] = value
                            }
                            localSettings = localSettings.copy(bands = updatedBands)
                        },
                        onValueChangeFinished = { onSettingsChange(localSettings) },
                        valueRange = -12f..12f,
                        steps = 23,
                        enabled = localSettings.enabled
                    )
                }
            }
            Text(
                uiText(language, "eq.description"),
                color = SonoraMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
