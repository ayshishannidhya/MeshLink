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
package com.meshlink.core.domain.repository

import com.meshlink.core.common.PowerMode
import com.meshlink.core.common.ScanInterval
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val displayName: Flow<String>
    val avatarUri: Flow<String?>
    val bleEnabled: Flow<Boolean>
    val wifiDirectEnabled: Flow<Boolean>
    val lanEnabled: Flow<Boolean>
    val powerMode: Flow<PowerMode>
    val scanInterval: Flow<ScanInterval>

    suspend fun setDisplayName(name: String)
    suspend fun setAvatarUri(uri: String?)
    suspend fun setBleEnabled(enabled: Boolean)
    suspend fun setWifiDirectEnabled(enabled: Boolean)
    suspend fun setLanEnabled(enabled: Boolean)
    suspend fun setPowerMode(mode: PowerMode)
    suspend fun setScanInterval(interval: ScanInterval)
    suspend fun clearAllPreferences()
}
