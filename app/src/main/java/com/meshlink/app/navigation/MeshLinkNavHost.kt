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

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meshlink.feature.chat.ui.ChatListScreen
import com.meshlink.feature.chat.ui.ChatScreen
import com.meshlink.feature.contacts.ui.ContactListScreen
import com.meshlink.feature.contacts.ui.DiscoveryScreen
import com.meshlink.feature.contacts.ui.QrPairingScreen
import com.meshlink.feature.devtools.ui.DevConsoleScreen
import com.meshlink.feature.meshmap.ui.MeshMapScreen
import com.meshlink.feature.settings.ui.SettingsScreen

/**
 * Top-level navigation graph for MeshLink.
 *
 * Routes are organized as:
 *  - chat_list â†’ chat/{peerId}
 *  - contacts â†’ discovery / qr_pairing
 *  - mesh_map
 *  - settings
 *  - dev_console
 */
@Composable
fun MeshLinkNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Route.ChatList.path,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Route.ChatList.path) {
            ChatListScreen(
                onNavigateToChat = { peerId ->
                    navController.navigate(Route.Chat.withArgs(peerId))
                },
                onNavigateToContacts = {
                    navController.navigate(Route.Contacts.path)
                },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings.path)
                },
                onNavigateToMeshMap = {
                    navController.navigate(Route.MeshMap.path)
                }
            )
        }

        composable(
            route = Route.Chat.path,
            arguments = Route.Chat.arguments
        ) {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.Contacts.path) {
            ContactListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDiscovery = {
                    navController.navigate(Route.Discovery.path)
                },
                onNavigateToQrPairing = {
                    navController.navigate(Route.QrPairing.path)
                },
                onNavigateToChat = { peerId ->
                    navController.navigate(Route.Chat.withArgs(peerId)) {
                        popUpTo(Route.ChatList.path)
                    }
                }
            )
        }

        composable(Route.Discovery.path) {
            DiscoveryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.QrPairing.path) {
            QrPairingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.MeshMap.path) {
            MeshMapScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.Settings.path) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDevConsole = {
                    navController.navigate(Route.DevConsole.path)
                }
            )
        }

        composable(Route.DevConsole.path) {
            DevConsoleScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
