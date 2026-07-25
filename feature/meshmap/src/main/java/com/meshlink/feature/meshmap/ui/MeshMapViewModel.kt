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
package com.meshlink.feature.meshmap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.core.domain.model.MeshTopology
import com.meshlink.core.domain.usecase.ObserveMeshTopologyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MeshMapUiState {
    data object Loading : MeshMapUiState
    data object Empty : MeshMapUiState
    data class Active(val topology: MeshTopology) : MeshMapUiState
}

@HiltViewModel
class MeshMapViewModel @Inject constructor(
    observeMeshTopologyUseCase: ObserveMeshTopologyUseCase
) : ViewModel() {

    val uiState: StateFlow<MeshMapUiState> = observeMeshTopologyUseCase()
        .map { topology ->
            if (topology.peerNodes.isEmpty()) {
                MeshMapUiState.Empty
            } else {
                MeshMapUiState.Active(topology)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MeshMapUiState.Loading
        )
}
