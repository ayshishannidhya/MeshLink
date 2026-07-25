/*
 * =============================================================================
 * MeshLink
 * Secure Offline Mesh Communication Platform
 *
 * Copyright (c) 2026 Ayshi Shannidhya Panda.
 * All Rights Reserved.
 *
 * MeshLink, the MeshLink Protocol, associated software, source code,
 * documentation, algorithms, and design architecture are proprietary
 * intellectual property of Ayshi Shannidhya Panda.
 *
 * Unauthorized reproduction, modification, distribution, or commercial
 * exploitation of any part of this software or protocol is prohibited
 * without prior written permission.
 *
 * Author  : Ayshi Shannidhya Panda
 * =============================================================================
 */
package com.meshlink.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.core.common.PowerMode
import com.meshlink.core.common.ScanInterval
import com.meshlink.core.domain.model.SettingsState
import com.meshlink.core.domain.repository.SettingsRepository
import com.meshlink.core.domain.usecase.ObserveSettingsUseCase
import com.meshlink.core.domain.usecase.PerformEmergencyWipeUseCase
import com.meshlink.core.domain.usecase.UpdateTransportSettingsUseCase
import com.meshlink.core.network.transport.TransportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Ready(val settings: SettingsState) : SettingsUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val updateTransportSettingsUseCase: UpdateTransportSettingsUseCase,
    private val performEmergencyWipeUseCase: PerformEmergencyWipeUseCase
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = observeSettingsUseCase()
        .map { SettingsUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState.Loading
        )

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            settingsRepository.setDisplayName(name)
        }
    }

    fun updateAvatarUri(uri: String?) {
        viewModelScope.launch {
            settingsRepository.setAvatarUri(uri)
        }
    }

    fun toggleTransport(type: TransportType, enabled: Boolean) {
        viewModelScope.launch {
            updateTransportSettingsUseCase(type, enabled)
        }
    }

    fun setPowerMode(mode: PowerMode) {
        viewModelScope.launch {
            settingsRepository.setPowerMode(mode)
        }
    }

    fun setScanInterval(interval: ScanInterval) {
        viewModelScope.launch {
            settingsRepository.setScanInterval(interval)
        }
    }

    fun performEmergencyWipe() {
        viewModelScope.launch {
            performEmergencyWipeUseCase()
        }
    }
}
