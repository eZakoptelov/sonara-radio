package com.example.sonora.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sonora.data.settings.SettingsRepository
import com.example.sonora.domain.repository.RadioRepository

class SonoraViewModelFactory(
    private val repository: RadioRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SonoraViewModel::class.java)) {
            return SonoraViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
