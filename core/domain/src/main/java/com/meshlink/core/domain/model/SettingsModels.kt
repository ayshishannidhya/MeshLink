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
package com.meshlink.core.domain.model

import com.meshlink.core.common.PowerMode
import com.meshlink.core.common.ScanInterval

data class SettingsState(
    val displayName: String,
    val avatarUri: String?,
    val fingerprint: String,
    val publicKeyHex: String,
    val bleEnabled: Boolean,
    val wifiDirectEnabled: Boolean,
    val lanEnabled: Boolean,
    val powerMode: PowerMode,
    val scanInterval: ScanInterval
)
