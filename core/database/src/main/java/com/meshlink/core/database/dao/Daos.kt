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
package com.meshlink.core.database.dao

import androidx.room.*
import com.meshlink.core.database.entity.ConversationEntity
import com.meshlink.core.database.entity.MessageEntity
import com.meshlink.core.database.entity.PeerEntity
import com.meshlink.core.database.entity.PendingPacketEntity
import kotlinx.coroutines.flow.Flow

/** DAO for chat messages with reactive Flow queries. */
@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: Int)

    @Query("SELECT COUNT(*) FROM messages")
    suspend fun getMessageCount(): Int

    @Query("DELETE FROM messages WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}

/** DAO for peer identities. */
@Dao
interface PeerDao {
    @Query("SELECT * FROM peers ORDER BY lastSeen DESC")
    fun getAllPeers(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers WHERE isFavorite = 1 ORDER BY displayName ASC")
    fun getFavorites(): Flow<List<PeerEntity>>

    @Query("SELECT * FROM peers WHERE id = :id")
    suspend fun getPeerById(id: String): PeerEntity?

    @Query("SELECT * FROM peers WHERE publicKeyHex = :publicKeyHex")
    suspend fun getPeerByPublicKey(publicKeyHex: String): PeerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeer(peer: PeerEntity)

    @Update
    suspend fun updatePeer(peer: PeerEntity)

    @Query("UPDATE peers SET lastSeen = :lastSeen WHERE id = :id")
    suspend fun updateLastSeen(id: String, lastSeen: Long)

    @Delete
    suspend fun deletePeer(peer: PeerEntity)

    @Query("DELETE FROM peers")
    suspend fun deleteAll()
}

/** DAO for conversations. */
@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE type = :type ORDER BY lastMessageTimestamp DESC")
    fun getConversationsByType(type: Int): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET unreadCount = unreadCount + 1 WHERE id = :id")
    suspend fun incrementUnread(id: String)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun clearUnread(id: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}

/** DAO for store-and-forward queue. */
@Dao
interface PendingPacketDao {
    @Query("SELECT * FROM pending_packets WHERE expiresAt > :now ORDER BY priority ASC, enqueuedAt ASC")
    suspend fun getActivePackets(now: Long = System.currentTimeMillis()): List<PendingPacketEntity>

    @Query("SELECT * FROM pending_packets WHERE recipientId = :recipientId AND expiresAt > :now")
    suspend fun getPacketsForRecipient(recipientId: String, now: Long = System.currentTimeMillis()): List<PendingPacketEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacket(packet: PendingPacketEntity)

    @Query("DELETE FROM pending_packets WHERE packetId = :packetId")
    suspend fun deletePacket(packetId: String)

    @Query("DELETE FROM pending_packets WHERE expiresAt < :now")
    suspend fun deleteExpired(now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM pending_packets")
    suspend fun getQueueSize(): Int
}
