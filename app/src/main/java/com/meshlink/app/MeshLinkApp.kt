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
package com.meshlink.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * MeshLink Application entry point.
 *
 * Initializes dependency injection via Hilt, sets up logging,
 * and creates notification channels for the mesh relay service.
 */
@HiltAndroidApp
class MeshLinkApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupLogging()
        createNotificationChannels()
    }

    private fun setupLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val meshChannel = NotificationChannel(
            CHANNEL_MESH,
            getString(R.string.notification_channel_mesh),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_mesh_description)
            setShowBadge(false)
        }

        val messageChannel = NotificationChannel(
            CHANNEL_MESSAGES,
            getString(R.string.notification_channel_messages),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            setShowBadge(true)
        }

        manager.createNotificationChannels(listOf(meshChannel, messageChannel))
    }

    companion object {
        const val CHANNEL_MESH = "mesh_relay"
        const val CHANNEL_MESSAGES = "messages"
    }
}
