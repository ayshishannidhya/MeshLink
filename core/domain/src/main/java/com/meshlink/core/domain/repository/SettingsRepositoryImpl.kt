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

import com.meshlink.core.common.MeshPreferences
import com.meshlink.core.common.PowerMode
import com.meshlink.core.common.ScanInterval
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val meshPreferences: MeshPreferences
) : SettingsRepository {
    override val displayName: Flow<String> = meshPreferences.displayName
    override val avatarUri: Flow<String?> = meshPreferences.avatarUri
    override val bleEnabled: Flow<Boolean> = meshPreferences.bleEnabled
    override val wifiDirectEnabled: Flow<Boolean> = meshPreferences.wifiDirectEnabled
    override val lanEnabled: Flow<Boolean> = meshPreferences.lanEnabled
    override val powerMode: Flow<PowerMode> = meshPreferences.powerMode
    override val scanInterval: Flow<ScanInterval> = meshPreferences.scanInterval

    override suspend fun setDisplayName(name: String) = meshPreferences.setDisplayName(name)
    override suspend fun setAvatarUri(uri: String?) = meshPreferences.setAvatarUri(uri)
    override suspend fun setBleEnabled(enabled: Boolean) = meshPreferences.setBleEnabled(enabled)
    override suspend fun setWifiDirectEnabled(enabled: Boolean) = meshPreferences.setWifiDirectEnabled(enabled)
    override suspend fun setLanEnabled(enabled: Boolean) = meshPreferences.setLanEnabled(enabled)
    override suspend fun setPowerMode(mode: PowerMode) = meshPreferences.setPowerMode(mode)
    override suspend fun setScanInterval(interval: ScanInterval) = meshPreferences.setScanInterval(interval)
    override suspend fun clearAllPreferences() = meshPreferences.clearAll()
}
