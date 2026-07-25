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
package com.meshlink.core.domain.usecase

import com.meshlink.core.domain.model.SettingsState
import com.meshlink.core.domain.repository.IdentityRepository
import com.meshlink.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Observes all user settings as a single reactive [SettingsState] flow.
 *
 * Combines multiple DataStore preference flows with identity information
 * into a unified state object for the Settings UI. Uses nested [combine]
 * calls since kotlinx.coroutines `combine` supports at most 5 typed flows.
 *
 * Emits whenever any individual preference changes — zero polling.
 */
class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val identityRepository: IdentityRepository
) {
    operator fun invoke(): Flow<SettingsState> {
        // Split into groups of ≤5 flows, then combine the groups
        val identityFlow = combine(
            settingsRepository.displayName,
            settingsRepository.avatarUri
        ) { displayName, avatarUri ->
            Pair(displayName, avatarUri)
        }

        val transportFlow = combine(
            settingsRepository.bleEnabled,
            settingsRepository.wifiDirectEnabled,
            settingsRepository.lanEnabled
        ) { ble, wifi, lan ->
            Triple(ble, wifi, lan)
        }

        val profileFlow = combine(
            settingsRepository.powerMode,
            settingsRepository.scanInterval
        ) { power, scan ->
            Pair(power, scan)
        }

        return combine(
            identityFlow,
            transportFlow,
            profileFlow
        ) { identity, transport, profile ->
            SettingsState(
                displayName = identity.first,
                avatarUri = identity.second,
                fingerprint = identityRepository.getFingerprint(),
                publicKeyHex = identityRepository.getPublicKeyHex(),
                bleEnabled = transport.first,
                wifiDirectEnabled = transport.second,
                lanEnabled = transport.third,
                powerMode = profile.first,
                scanInterval = profile.second
            )
        }
    }
}
