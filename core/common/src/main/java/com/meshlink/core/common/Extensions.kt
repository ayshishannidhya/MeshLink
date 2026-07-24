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

/**
 * Extension functions used across the MeshLink codebase.
 */

/** Convert ByteArray to hex string for display/logging. */
fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

/** Convert hex string back to ByteArray. */
fun String.hexToByteArray(): ByteArray {
    require(length % 2 == 0) { "Hex string must have even length" }
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

/** Truncated hex for peer IDs in logs (first 8 chars). */
fun ByteArray.toShortHex(): String = toHexString().take(8)

/** Safe equality check for byte arrays (constant-time to prevent timing attacks). */
fun ByteArray.constantTimeEquals(other: ByteArray): Boolean {
    if (size != other.size) return false
    var result = 0
    for (i in indices) {
        result = result or (this[i].toInt() xor other[i].toInt())
    }
    return result == 0
}

/** Current time in milliseconds. */
fun currentTimeMillis(): Long = System.currentTimeMillis()

/** Clamp a value to a range. */
fun Int.clamp(min: Int, max: Int): Int = coerceIn(min, max)
fun Long.clamp(min: Long, max: Long): Long = coerceIn(min, max)
fun Float.clamp(min: Float, max: Float): Float = coerceIn(min, max)
