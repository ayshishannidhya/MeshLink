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
package com.meshlink.app.di

import android.content.Context
import com.meshlink.core.crypto.*
import com.meshlink.core.database.MeshLinkDatabase
import com.meshlink.core.database.dao.*
import com.meshlink.core.mesh.MeshEngine
import com.meshlink.core.mesh.RateLimiter
import com.meshlink.core.mesh.flood.FloodController
import com.meshlink.core.mesh.routing.MeshRouter
import com.meshlink.core.mesh.routing.NeighborTable
import com.meshlink.core.mesh.store.StoreForwardManager
import com.meshlink.core.network.packet.PacketCodec
import com.meshlink.core.network.transport.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Hilt module providing all core dependencies.
 * Everything flows from here through constructor injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // â”€â”€ Crypto â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Provides
    @Singleton
    fun provideKeyPairGenerator(): MeshKeyPairGenerator = MeshKeyPairGenerator()

    @Provides
    @Singleton
    fun provideCipher(): ChaCha20Poly1305Cipher = ChaCha20Poly1305Cipher()

    @Provides
    @Singleton
    fun provideHkdf(): HkdfSha256 = HkdfSha256()

    // â”€â”€ Network â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Provides
    @Singleton
    fun providePacketCodec(): PacketCodec = PacketCodec()

    @Provides
    @Singleton
    @IntoSet
    fun provideBleTransport(
        @ApplicationContext context: Context
    ): MeshTransport {
        val peerId = com.meshlink.core.common.MeshConstants.generateId()
        return BleTransport(context, peerId)
    }

    @Provides
    @Singleton
    @IntoSet
    fun provideWifiDirectTransport(
        @ApplicationContext context: Context
    ): MeshTransport {
        val peerId = com.meshlink.core.common.MeshConstants.generateId()
        return WifiDirectTransport(context, peerId)
    }

    @Provides
    @Singleton
    @IntoSet
    fun provideLanTransport(
        @ApplicationContext context: Context
    ): MeshTransport {
        val peerId = com.meshlink.core.common.MeshConstants.generateId()
        return LanTransport(context, peerId)
    }

    @Provides
    @Singleton
    fun provideTransportManager(
        transports: Set<@JvmSuppressWildcards MeshTransport>
    ): TransportManager = TransportManager(transports)

    // â”€â”€ Mesh â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Provides
    @Singleton
    fun provideNeighborTable(): NeighborTable = NeighborTable()

    @Provides
    @Singleton
    fun provideMeshRouter(neighborTable: NeighborTable): MeshRouter = MeshRouter(neighborTable)

    @Provides
    @Singleton
    fun provideFloodController(): FloodController = FloodController()

    @Provides
    @Singleton
    fun provideStoreForwardManager(): StoreForwardManager = StoreForwardManager()

    @Provides
    @Singleton
    fun provideRateLimiter(): RateLimiter = RateLimiter()

    @Provides
    @Singleton
    fun provideMeshEngine(
        transports: Set<@JvmSuppressWildcards MeshTransport>,
        router: MeshRouter,
        floodController: FloodController,
        storeForwardManager: StoreForwardManager,
        neighborTable: NeighborTable,
        packetCodec: PacketCodec,
        rateLimiter: RateLimiter
    ): MeshEngine = MeshEngine(
        transports, router, floodController, storeForwardManager,
        neighborTable, packetCodec, rateLimiter
    )

    // â”€â”€ Database â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MeshLinkDatabase {
        // In production, passphrase comes from Android Keystore
        val passphrase = "meshlink-dev-key".toByteArray()
        return MeshLinkDatabase.create(context, passphrase)
    }

    @Provides
    fun provideMessageDao(db: MeshLinkDatabase): MessageDao = db.messageDao()

    @Provides
    fun providePeerDao(db: MeshLinkDatabase): PeerDao = db.peerDao()

    @Provides
    fun provideConversationDao(db: MeshLinkDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun providePendingPacketDao(db: MeshLinkDatabase): PendingPacketDao = db.pendingPacketDao()
}
