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
// Top-level build file for MeshLink
// Decentralized Offline Mesh Communication Platform

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
