package com.example.sonora.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sonora.R
import com.example.sonora.domain.model.RadioLanguage
import com.example.sonora.presentation.localization.uiText
import java.util.Locale
import com.example.sonora.presentation.theme.SonoraBackground
import com.example.sonora.presentation.theme.SonoraMuted
import com.example.sonora.presentation.theme.SonoraPrimary
import com.example.sonora.presentation.theme.SonoraSecondary
import com.example.sonora.presentation.theme.SonoraSurface
import com.example.sonora.presentation.theme.SonoraText

@Composable
fun LanguageSetupScreen(
    onLanguageSelected: (RadioLanguage) -> Unit
) {
    var selectedLanguageName by rememberSaveable {
        mutableStateOf(
            RadioLanguage.entries.firstOrNull { it.code == Locale.getDefault().language }?.name
                ?: RadioLanguage.RUSSIAN.name
        )
    }
    val selectedLanguage = RadioLanguage.entries.firstOrNull { it.name == selectedLanguageName }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SonoraBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.sonara_brand_mark),
            contentDescription = "SONARA Online Radio",
            modifier = Modifier
                .size(86.dp)
                .clip(RoundedCornerShape(24.dp))
        )
        Text(
            "SONARA",
            color = SonoraText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Language, contentDescription = null, tint = SonoraSecondary, modifier = Modifier.size(34.dp))
            Text(
                uiText(selectedLanguage, "setup.title"),
                color = SonoraText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                uiText(selectedLanguage, "setup.description"),
                color = SonoraMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(SonoraSurface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioLanguage.entries.chunked(2).forEach { rowLanguages ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowLanguages.forEach { language ->
                        FilterChip(
                            selected = selectedLanguage == language,
                            onClick = { selectedLanguageName = language.name },
                            label = {
                                Text(
                                    "${language.flag}  ${language.title}",
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowLanguages.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Button(
            onClick = { selectedLanguage?.let(onLanguageSelected) },
            enabled = selectedLanguage != null,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SonoraPrimary,
                contentColor = SonoraBackground,
                disabledContainerColor = SonoraPrimary.copy(alpha = 0.25f),
                disabledContentColor = SonoraMuted
            )
        ) {
            Text(uiText(selectedLanguage, "setup.continue"), fontWeight = FontWeight.Bold)
        }
        Text(
            uiText(selectedLanguage, "setup.hint"),
            color = SonoraMuted,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun LoadingSettingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(SonoraBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = SonoraPrimary)
        Spacer(Modifier.height(14.dp))
        Text(uiText(RadioLanguage.entries.firstOrNull { it.code == Locale.getDefault().language }, "setup.loading"), color = SonoraMuted)
    }
}
