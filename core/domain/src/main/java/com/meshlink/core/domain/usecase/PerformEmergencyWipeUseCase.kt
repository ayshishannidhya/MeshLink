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

import android.content.Context
import com.meshlink.core.database.dao.ConversationDao
import com.meshlink.core.database.dao.MessageDao
import com.meshlink.core.database.dao.PeerDao
import com.meshlink.core.database.dao.PendingPacketDao
import com.meshlink.core.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class PerformEmergencyWipeUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageDao: MessageDao,
    private val peerDao: PeerDao,
    private val conversationDao: ConversationDao,
    private val pendingPacketDao: PendingPacketDao,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        // Clear DB
        messageDao.deleteAll()
        peerDao.deleteAll()
        conversationDao.deleteAll()
        pendingPacketDao.deleteAll()
        
        // Clear preferences
        settingsRepository.clearAllPreferences()

        // Clear files
        deleteRecursively(context.cacheDir)
        deleteRecursively(context.filesDir)
    }

    private fun deleteRecursively(fileOrDir: File) {
        if (fileOrDir.isDirectory) {
            fileOrDir.listFiles()?.forEach { child ->
                deleteRecursively(child)
            }
        }
        if (fileOrDir != context.cacheDir && fileOrDir != context.filesDir) {
            fileOrDir.delete()
        }
    }
}
