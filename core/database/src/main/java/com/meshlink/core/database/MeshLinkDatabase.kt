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
package com.meshlink.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.meshlink.core.database.dao.ConversationDao
import com.meshlink.core.database.dao.MessageDao
import com.meshlink.core.database.dao.PeerDao
import com.meshlink.core.database.dao.PendingPacketDao
import com.meshlink.core.database.entity.ConversationEntity
import com.meshlink.core.database.entity.MessageEntity
import com.meshlink.core.database.entity.PeerEntity
import com.meshlink.core.database.entity.PendingPacketEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Room database encrypted with SQLCipher.
 *
 * ## Security
 * All data at rest is encrypted using SQLCipher 4 with a 256-bit key.
 * The passphrase is derived from Android Keystore-backed material,
 * making it inaccessible even on rooted devices.
 *
 * ## Tables
 * - messages: Chat message history (encrypted content)
 * - peers: Known peer identities and metadata
 * - conversations: Chat threads with last message info
 * - pending_packets: Store-and-forward queue (persisted across app restarts)
 */
@Database(
    entities = [
        MessageEntity::class,
        PeerEntity::class,
        ConversationEntity::class,
        PendingPacketEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MeshLinkDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun peerDao(): PeerDao
    abstract fun conversationDao(): ConversationDao
    abstract fun pendingPacketDao(): PendingPacketDao

    companion object {
        private const val DATABASE_NAME = "meshlink.db"

        /**
         * Create an encrypted database instance.
         *
         * @param context Application context
         * @param passphrase Encryption passphrase (should come from Keystore)
         */
        fun create(context: Context, passphrase: ByteArray): MeshLinkDatabase {
            System.loadLibrary("sqlcipher")
            val factory = SupportOpenHelperFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                MeshLinkDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
