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

/**
 * Domain models â€” pure Kotlin, framework-agnostic.
 * These are what the UI layer works with.
 */

/** A chat message as seen by the application. */
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val status: MessageStatus,
    val hopCount: Int = 0,
    val type: MessageType = MessageType.TEXT
)

enum class MessageStatus {
    QUEUED,     // Waiting to send
    RELAYED,    // Sent to at least one relay
    DELIVERED   // Confirmed received by recipient
    // No READ status â€” privacy first
}

enum class MessageType {
    TEXT, IMAGE, VOICE, FILE, SOS
}

/** A peer identity (contact). */
data class PeerIdentity(
    val id: String,
    val publicKeyHex: String,
    val signingKeyHex: String,
    val displayName: String,
    val avatarUri: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0,
    val isFavorite: Boolean = false,
    val trustScore: Float = 0.5f,
    val rssi: Int? = null,
    val transport: String? = null
)

/** A conversation thread. */
data class Conversation(
    val id: String,
    val type: ConversationType,
    val title: String?,
    val participantIds: List<String>,
    val lastMessagePreview: String? = null,
    val lastMessageTimestamp: Long = 0,
    val unreadCount: Int = 0,
    val channelPriority: Int = 2
)

enum class ConversationType {
    PRIVATE,  // 1:1
    GROUP,
    CHANNEL   // Broadcast channel (Emergency, Medical, etc.)
}

/** Mesh channel definition. */
data class MeshChannel(
    val id: String,
    val name: String,
    val priority: Int,
    val icon: String
) {
    companion object {
        val DEFAULTS = listOf(
            MeshChannel("emergency", "Emergency", 0, "ðŸš¨"),
            MeshChannel("medical", "Medical", 0, "ðŸ¥"),
            MeshChannel("announcements", "Announcements", 1, "ðŸ“¢"),
            MeshChannel("food", "Food & Water", 2, "ðŸž"),
            MeshChannel("lost_found", "Lost & Found", 2, "ðŸ”"),
            MeshChannel("general", "General", 3, "ðŸ’¬")
        )
    }
}

/** The local user's identity. */
data class LocalIdentity(
    val peerId: ByteArray,
    val x25519PublicKey: ByteArray,
    val ed25519PublicKey: ByteArray,
    val displayName: String,
    val fingerprintHex: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalIdentity) return false
        return peerId.contentEquals(other.peerId)
    }
    override fun hashCode(): Int = peerId.contentHashCode()
}
