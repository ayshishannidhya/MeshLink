package com.meshlink.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.meshlink.app.navigation.MeshLinkNavHost
import com.meshlink.app.service.MeshRelayService
import com.meshlink.core.ui.theme.MeshLinkTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Single Activity host for the entire MeshLink application.
 * All navigation is handled via Jetpack Compose Navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Launcher that requests all mesh-related permissions at once.
     * After the user responds, starts the mesh relay if granted.
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            Timber.i("All mesh permissions granted — starting relay service")
            startMeshRelay()
        } else {
            val denied = results.filter { !it.value }.keys
            Timber.w("Some permissions denied: $denied — mesh will be limited")
            // Start anyway — transports will gracefully fail for denied permissions
            startMeshRelay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request permissions first, then start mesh service
        requestMeshPermissions()

        setContent {
            MeshLinkTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MeshLinkNavHost()
                }
            }
        }
    }

    /**
     * Checks and requests all permissions needed for mesh transports.
     * - Android 12+ (API 31): BLUETOOTH_SCAN, BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE
     * - Android 13+ (API 33): NEARBY_WIFI_DEVICES (for Wi-Fi Direct)
     * - All versions: ACCESS_FINE_LOCATION (for BLE scanning)
     */
    private fun requestMeshPermissions() {
        val required = mutableListOf<String>()

        // BLE permissions (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            required.add(Manifest.permission.BLUETOOTH_SCAN)
            required.add(Manifest.permission.BLUETOOTH_CONNECT)
            required.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        // Wi-Fi Direct peer discovery (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        // Location — needed for BLE scanning on Android 6-11
        required.add(Manifest.permission.ACCESS_FINE_LOCATION)
        required.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        // Notification permission (Android 13+) — for foreground service notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Filter to only permissions not yet granted
        val notGranted = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            // All already granted — start immediately
            Timber.i("All mesh permissions already granted")
            startMeshRelay()
        } else {
            Timber.i("Requesting ${notGranted.size} mesh permissions: $notGranted")
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    /**
     * Starts the MeshRelayService as a foreground service.
     * Wrapped in try-catch because on Android 14+ FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
     * requires BLUETOOTH_CONNECT permission to be granted first.
     * If permissions aren't granted yet, the service will start later
     * when the user grants them.
     */
    private fun startMeshRelay() {
        try {
            val intent = Intent(this, MeshRelayService::class.java).apply {
                action = MeshRelayService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to start MeshRelayService — permissions may not be granted yet")
        }
    }
}

