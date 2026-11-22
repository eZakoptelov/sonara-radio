package com.example.sonora.presentation.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sonora.R
import com.example.sonora.alarm.RadioAlarmConfig
import com.example.sonora.data.settings.AppTheme
import com.example.sonora.data.settings.BufferMode
import com.example.sonora.domain.model.RadioLanguage
import com.example.sonora.domain.model.RadioStation
import com.example.sonora.presentation.SonoraUiState
import com.example.sonora.presentation.localization.uiText
import com.example.sonora.presentation.theme.SonoraMuted
import com.example.sonora.presentation.theme.SonoraPrimary
import com.example.sonora.presentation.theme.SonoraSurface
import com.example.sonora.presentation.theme.SonoraText
import com.example.sonora.presentation.theme.paletteFor

@Composable
fun SettingsScreen(
    state: SonoraUiState,
    sleepTimerMinutes: Int,
    onBufferModeChange: (BufferMode) -> Unit,
    onSleepTimerChange: (Int) -> Unit,
    onOpenEqualizer: () -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onRadioLanguageChange: (RadioLanguage) -> Unit,
    alarmConfig: RadioAlarmConfig?,
    onSetAlarm: (Int, Int, RadioStation, Boolean) -> Boolean,
    onCancelAlarm: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit
) {
    val context = LocalContext.current
    var alarmHour by rememberSaveable(alarmConfig?.hour) {
        mutableStateOf(alarmConfig?.hour ?: 7)
    }
    var alarmMinute by rememberSaveable(alarmConfig?.minute) {
        mutableStateOf(alarmConfig?.minute ?: 0)
    }
    var repeatDaily by rememberSaveable(alarmConfig?.repeatDaily) {
        mutableStateOf(alarmConfig?.repeatDaily ?: true)
    }
    val stationOptions = remember(
        state.favorites,
        state.popularStations,
        state.currentStation,
        alarmConfig?.station
    ) {
        (state.favorites + listOfNotNull(state.currentStation, alarmConfig?.station) + state.popularStations)
            .distinctBy { it.id }
    }
    var selectedStationId by rememberSaveable(alarmConfig?.station?.id) {
        mutableStateOf(alarmConfig?.station?.id ?: stationOptions.firstOrNull()?.id.orEmpty())
    }
    var stationMenuExpanded by remember { mutableStateOf(false) }
    var exactAlarmPermissionRequired by rememberSaveable { mutableStateOf(false) }
    val selectedStation = stationOptions.firstOrNull { it.id == selectedStationId }
        ?: alarmConfig?.station
        ?: stationOptions.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        Text(uiText(state.radioLanguage, "settings.title"), color = SonoraText, style = MaterialTheme.typography.headlineMedium)
        Text(uiText(state.radioLanguage, "settings.subtitle"), color = SonoraMuted, style = MaterialTheme.typography.bodySmall)

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Language, contentDescription = null, tint = SonoraPrimary)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(uiText(state.radioLanguage, "settings.language"), color = SonoraText, style = MaterialTheme.typography.titleMedium)
                    Text(
                        uiText(state.radioLanguage, "settings.current", "${state.radioLanguage?.flag.orEmpty()} ${state.radioLanguage?.title ?: uiText(state.radioLanguage, "home.not_selected")}"),
                        color = SonoraMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            RadioLanguage.entries.chunked(2).forEach { rowLanguages ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowLanguages.forEach { language ->
                        FilterChip(
                            selected = state.radioLanguage == language,
                            onClick = { onRadioLanguageChange(language) },
                            label = { Text("${language.flag} ${language.title}", maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowLanguages.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Text(
                uiText(state.radioLanguage, "settings.language_description"),
                color = SonoraMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Palette, contentDescription = null, tint = SonoraPrimary)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(uiText(state.radioLanguage, "settings.appearance"), color = SonoraText, style = MaterialTheme.typography.titleMedium)
                    Text(uiText(state.radioLanguage, "settings.appearance_description"), color = SonoraMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            AppTheme.entries.chunked(2).forEach { rowThemes ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowThemes.forEach { theme ->
                        FilterChip(
                            selected = state.appTheme == theme,
                            onClick = { onThemeChange(theme) },
                            label = {
                                Text(
                                    uiText(
                                        state.radioLanguage,
                                        when (theme) {
                                            AppTheme.NEON -> "theme.neon"
                                            AppTheme.OCEAN -> "theme.ocean"
                                            AppTheme.AMOLED -> "theme.amoled"
                                            AppTheme.LIGHT -> "theme.light"
                                            AppTheme.SUNSET -> "theme.sunset"
                                            AppTheme.FOREST -> "theme.forest"
                                            AppTheme.ROSE -> "theme.rose"
                                            AppTheme.GRAPHITE -> "theme.graphite"
                                        }
                                    )
                                )
                            },
                            leadingIcon = {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(paletteFor(theme).primary)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowThemes.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Text(
                uiText(
                    state.radioLanguage,
                    when (state.appTheme) {
                        AppTheme.NEON -> "theme.neon_desc"
                        AppTheme.OCEAN -> "theme.ocean_desc"
                        AppTheme.AMOLED -> "theme.amoled_desc"
                        AppTheme.LIGHT -> "theme.light_desc"
                        AppTheme.SUNSET -> "theme.sunset_desc"
                        AppTheme.FOREST -> "theme.forest_desc"
                        AppTheme.ROSE -> "theme.rose_desc"
                        AppTheme.GRAPHITE -> "theme.graphite_desc"
                    }
                ),
                color = SonoraMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenEqualizer)
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = SonoraPrimary)
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(uiText(state.radioLanguage, "settings.equalizer"), color = SonoraText, style = MaterialTheme.typography.titleMedium)
                    Text(uiText(state.radioLanguage, "settings.equalizer_description"), color = SonoraMuted, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = uiText(state.radioLanguage, "settings.open_equalizer"), tint = SonoraMuted)
            }
        }

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = SonoraPrimary)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(uiText(state.radioLanguage, "settings.buffering"), color = SonoraText, style = MaterialTheme.typography.titleMedium)
                    Text(uiText(state.radioLanguage, "settings.buffering_description"), color = SonoraMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BufferMode.entries.forEach { mode ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        FilterChip(
                            selected = state.bufferMode == mode,
                            onClick = { onBufferModeChange(mode) },
                            label = {
                                Text(
                                    uiText(
                                        state.radioLanguage,
                                        when (mode) {
                                            BufferMode.AUTO -> "buffer.auto"
                                            BufferMode.LOW_LATENCY -> "buffer.low"
                                            BufferMode.BALANCED -> "buffer.balanced"
                                            BufferMode.STABLE -> "buffer.stable"
                                        }
                                    )
                                )
                            }
                        )
                        Text(
                            uiText(
                                state.radioLanguage,
                                when (mode) {
                                    BufferMode.AUTO -> "buffer.auto_desc"
                                    BufferMode.LOW_LATENCY -> "buffer.low_desc"
                                    BufferMode.BALANCED -> "buffer.balanced_desc"
                                    BufferMode.STABLE -> "buffer.stable_desc"
                                }
                            ),
                            color = SonoraMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Text(
                uiText(state.radioLanguage, "settings.buffer_restart"),
                color = SonoraMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Alarm, contentDescription = null, tint = SonoraPrimary)
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(uiText(state.radioLanguage, "alarm.title"), color = SonoraText, style = MaterialTheme.typography.titleMedium)
                    Text(uiText(state.radioLanguage, "alarm.description"), color = SonoraMuted, style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(uiText(state.radioLanguage, "alarm.time"), color = SonoraMuted, style = MaterialTheme.typography.labelMedium)
                    Text(
                        String.format(java.util.Locale.ROOT, "%02d:%02d", alarmHour, alarmMinute),
                        color = SonoraText,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                TextButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                alarmHour = hour
                                alarmMinute = minute
                            },
                            alarmHour,
                            alarmMinute,
                            true
                        ).show()
                    }
                ) {
                    Text(uiText(state.radioLanguage, "alarm.change_time"), color = SonoraPrimary)
                }
            }

            Text(uiText(state.radioLanguage, "alarm.station"), color = SonoraMuted, style = MaterialTheme.typography.labelMedium)
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(com.example.sonora.presentation.theme.SonoraSurfaceElevated)
                        .clickable(enabled = stationOptions.isNotEmpty()) {
                            stationMenuExpanded = true
                        }
                        .padding(horizontal = 13.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Radio, contentDescription = null, tint = SonoraPrimary)
                    Spacer(Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selectedStation?.name ?: uiText(state.radioLanguage, "alarm.no_stations"),
                            color = SonoraText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                        Text(
                            selectedStation?.country?.ifBlank { uiText(state.radioLanguage, "alarm.radio_stream") }
                                ?: uiText(state.radioLanguage, "alarm.load_catalog"),
                            color = SonoraMuted,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SonoraMuted)
                }
                DropdownMenu(
                    expanded = stationMenuExpanded,
                    onDismissRequest = { stationMenuExpanded = false },
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    stationOptions.take(40).forEach { station ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(station.name, maxLines = 1)
                                    if (station.country.isNotBlank()) {
                                        Text(station.country, color = SonoraMuted, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            },
                            onClick = {
                                selectedStationId = station.id
                                stationMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Text(
                uiText(state.radioLanguage, "alarm.repeat_mode"),
                color = SonoraMuted,
                style = MaterialTheme.typography.labelMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = repeatDaily,
                    onClick = { repeatDaily = true },
                    label = { Text(uiText(state.radioLanguage, "alarm.repeat_daily")) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !repeatDaily,
                    onClick = { repeatDaily = false },
                    label = { Text(uiText(state.radioLanguage, "alarm.one_time")) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (alarmConfig != null) {
                Text(
                    uiText(
                        state.radioLanguage,
                        if (alarmConfig.repeatDaily) "alarm.active_daily" else "alarm.active_once",
                        String.format(java.util.Locale.ROOT, "%02d:%02d", alarmConfig.hour, alarmConfig.minute),
                        alarmConfig.station.name
                    ),
                    color = SonoraPrimary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val station = selectedStation
                        if (station != null) {
                            exactAlarmPermissionRequired = !onSetAlarm(alarmHour, alarmMinute, station, repeatDaily)
                        }
                    },
                    enabled = selectedStation != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        uiText(
                            state.radioLanguage,
                            if (alarmConfig == null) "alarm.set" else "alarm.save"
                        ),
                        maxLines = 1
                    )
                }
                if (alarmConfig != null) {
                    TextButton(
                        onClick = {
                            onCancelAlarm()
                            exactAlarmPermissionRequired = false
                        }
                    ) {
                        Text(uiText(state.radioLanguage, "alarm.disable"), color = SonoraMuted)
                    }
                }
            }

            if (selectedStation == null) {
                Text(
                    uiText(state.radioLanguage, "alarm.load_catalog"),
                    color = SonoraMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (exactAlarmPermissionRequired) {
                Text(
                    uiText(state.radioLanguage, "alarm.permission_description"),
                    color = SonoraMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = onRequestExactAlarmPermission) {
                    Text(uiText(state.radioLanguage, "alarm.permission_button"), color = SonoraPrimary)
                }
            }
        }

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bedtime, contentDescription = null, tint = SonoraPrimary)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(uiText(state.radioLanguage, "settings.sleep_timer"), color = SonoraText, style = MaterialTheme.typography.titleMedium)
                    Text(uiText(state.radioLanguage, "settings.sleep_description"), color = SonoraMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(0, 15, 30, 45, 60).forEach { minutes ->
                    FilterChip(
                        selected = sleepTimerMinutes == minutes,
                        onClick = { onSleepTimerChange(minutes) },
                        label = { Text(if (minutes == 0) uiText(state.radioLanguage, "settings.off") else uiText(state.radioLanguage, "settings.minutes", minutes)) }
                    )
                }
            }
            if (sleepTimerMinutes > 0) {
                Text(uiText(state.radioLanguage, "settings.stops_in", sleepTimerMinutes), color = SonoraPrimary, style = MaterialTheme.typography.bodySmall)
            }
        }

        SettingsCard {
            Image(
                painter = painterResource(R.drawable.sonara_brand_logo),
                contentDescription = "SONARA Online Radio",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Fit
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = SonoraPrimary)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(uiText(state.radioLanguage, "settings.about"), color = SonoraText, style = MaterialTheme.typography.titleMedium)
                    Text(uiText(state.radioLanguage, "settings.version"), color = SonoraMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                uiText(state.radioLanguage, "settings.catalog_info"),
                color = SonoraMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SonoraSurface, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}
