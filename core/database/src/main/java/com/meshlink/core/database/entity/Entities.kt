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
package com.meshlink.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted chat message.
 * Content is stored encrypted â€” only the owning device can decrypt.
 */
@Entity(
    tableName = "messages",
    indices = [
        Index("conversationId", "timestamp"),
        Index("senderId"),
        Index("status")
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val encryptedContent: ByteArray,
    val timestamp: Long,
    val status: Int,  // 0=queued, 1=relayed, 2=delivered
    val hopCount: Int = 0,
    val type: Int = 0,  // 0=text, 1=image, 2=voice, 3=file
    val priority: Int = 2
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageEntity) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}

/**
 * Known peer identity.
 * Stores public key, display name, and trust metadata.
 */
@Entity(
    tableName = "peers",
    indices = [Index("publicKeyHex", unique = true)]
)
data class PeerEntity(
    @PrimaryKey
    val id: String,
    val publicKeyHex: String,
    val signingKeyHex: String,
    val displayName: String,
    val avatarUri: String? = null,
    val lastSeen: Long = 0,
    val isFavorite: Boolean = false,
    val trustScore: Float = 0.5f,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Chat conversation (1:1 or group/channel).
 */
@Entity(
    tableName = "conversations",
    indices = [Index("lastMessageTimestamp")]
)
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val type: Int,  // 0=private, 1=group, 2=channel
    val title: String? = null,
    val participantIds: String,  // Comma-separated peer IDs
    val lastMessagePreview: String? = null,
    val lastMessageTimestamp: Long = 0,
    val unreadCount: Int = 0,
    val channelPriority: Int = 2  // For mesh channels
)

/**
 * Persisted store-and-forward packet (survives app restart).
 */
@Entity(
    tableName = "pending_packets",
    indices = [Index("expiresAt")]
)
data class PendingPacketEntity(
    @PrimaryKey
    val packetId: String,
    val recipientId: String,
    val encryptedPayload: ByteArray,
    val priority: Int,
    val enqueuedAt: Long,
    val expiresAt: Long,
    val retryCount: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PendingPacketEntity) return false
        return packetId == other.packetId
    }
    override fun hashCode(): Int = packetId.hashCode()
}
