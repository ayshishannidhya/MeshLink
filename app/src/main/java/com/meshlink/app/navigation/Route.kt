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
package com.meshlink.app.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Type-safe route definitions for MeshLink navigation.
 * Each route defines its path pattern and any required arguments.
 */
sealed class Route(val path: String) {

    data object ChatList : Route("chat_list")

    data object Chat : Route("chat/{peerId}") {
        fun withArgs(peerId: String) = "chat/$peerId"
        val arguments = listOf(
            navArgument("peerId") { type = NavType.StringType }
        )
    }

    data object Contacts : Route("contacts")

    data object Discovery : Route("discovery")

    data object QrPairing : Route("qr_pairing")

    data object MeshMap : Route("mesh_map")

    data object Settings : Route("settings")

    data object DevConsole : Route("dev_console")
}
