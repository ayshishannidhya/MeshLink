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

import com.meshlink.core.domain.repository.MeshRepository
import com.meshlink.core.domain.repository.SettingsRepository
import com.meshlink.core.network.transport.TransportType
import javax.inject.Inject

class UpdateTransportSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val meshRepository: MeshRepository
) {
    suspend operator fun invoke(type: TransportType, enabled: Boolean) {
        when (type) {
            TransportType.BLE -> settingsRepository.setBleEnabled(enabled)
            TransportType.WIFI_DIRECT -> settingsRepository.setWifiDirectEnabled(enabled)
            TransportType.LAN -> settingsRepository.setLanEnabled(enabled)
        }
        meshRepository.setTransportEnabled(type, enabled)
    }
}
