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
package com.meshlink.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.meshlink.app.MainActivity
import com.meshlink.app.MeshLinkApp
import com.meshlink.app.R
import com.meshlink.core.mesh.MeshEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service that keeps the mesh relay alive when the app is backgrounded.
 *
 * This service:
 * - Maintains BLE advertising and scanning
 * - Continues relaying packets for other mesh nodes
 * - Processes store-and-forward queue
 * - Runs with FOREGROUND_SERVICE_CONNECTED_DEVICE type
 *
 * Battery impact is minimized through adaptive duty cycling
 * managed by the MeshEngine.
 */
@AndroidEntryPoint
class MeshRelayService : LifecycleService() {

    @Inject
    lateinit var meshEngine: MeshEngine

    override fun onCreate() {
        super.onCreate()
        Timber.d("MeshRelayService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> startMeshRelay()
            ACTION_STOP -> stopMeshRelay()
        }

        return START_STICKY
    }

    private fun startMeshRelay() {
        val notification = buildNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        lifecycleScope.launch {
            meshEngine.start()
        }

        Timber.i("Mesh relay started")
    }

    private fun stopMeshRelay() {
        lifecycleScope.launch {
            meshEngine.stop()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Timber.i("Mesh relay stopped")
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MeshLinkApp.CHANNEL_MESH)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_mesh_active))
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("MeshRelayService destroyed")
    }

    companion object {
        const val ACTION_START = "com.meshlink.START_RELAY"
        const val ACTION_STOP = "com.meshlink.STOP_RELAY"
        private const val NOTIFICATION_ID = 1001
    }
}
