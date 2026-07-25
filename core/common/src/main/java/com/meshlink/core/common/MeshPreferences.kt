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
package com.meshlink.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class PowerMode { LOW_POWER, BALANCED, HIGH_PERFORMANCE }
enum class ScanInterval { SLOW, NORMAL, FAST }

private val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "meshlink_settings")

class MeshPreferences(private val context: Context) {

    private val dataStore = context.preferencesDataStore

    companion object {
        val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        val KEY_AVATAR_URI = stringPreferencesKey("avatar_uri")
        val KEY_BLE_ENABLED = booleanPreferencesKey("ble_enabled")
        val KEY_WIFI_DIRECT_ENABLED = booleanPreferencesKey("wifi_direct_enabled")
        val KEY_LAN_ENABLED = booleanPreferencesKey("lan_enabled")
        val KEY_POWER_MODE = stringPreferencesKey("power_mode")
        val KEY_SCAN_INTERVAL = stringPreferencesKey("scan_interval")
    }

    val displayName: Flow<String> = dataStore.data.map { it[KEY_DISPLAY_NAME] ?: "MeshLink User" }
    val avatarUri: Flow<String?> = dataStore.data.map { it[KEY_AVATAR_URI] }
    val bleEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_BLE_ENABLED] ?: true }
    val wifiDirectEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_WIFI_DIRECT_ENABLED] ?: true }
    val lanEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_LAN_ENABLED] ?: false }
    val powerMode: Flow<PowerMode> = dataStore.data.map { 
        PowerMode.valueOf(it[KEY_POWER_MODE] ?: PowerMode.BALANCED.name) 
    }
    val scanInterval: Flow<ScanInterval> = dataStore.data.map { 
        ScanInterval.valueOf(it[KEY_SCAN_INTERVAL] ?: ScanInterval.NORMAL.name) 
    }

    suspend fun setDisplayName(name: String) {
        dataStore.edit { it[KEY_DISPLAY_NAME] = name }
    }

    suspend fun setAvatarUri(uri: String?) {
        dataStore.edit { prefs ->
            if (uri != null) prefs[KEY_AVATAR_URI] = uri
            else prefs.remove(KEY_AVATAR_URI)
        }
    }

    suspend fun setBleEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BLE_ENABLED] = enabled }
    }

    suspend fun setWifiDirectEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_WIFI_DIRECT_ENABLED] = enabled }
    }

    suspend fun setLanEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_LAN_ENABLED] = enabled }
    }

    suspend fun setPowerMode(mode: PowerMode) {
        dataStore.edit { it[KEY_POWER_MODE] = mode.name }
    }

    suspend fun setScanInterval(interval: ScanInterval) {
        dataStore.edit { it[KEY_SCAN_INTERVAL] = interval.name }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
