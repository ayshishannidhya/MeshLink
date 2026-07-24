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
package com.meshlink.core.crypto

import com.meshlink.core.common.MeshResult
import timber.log.Timber
import java.security.MessageDigest

/**
 * Noise Protocol Framework XX handshake implementation.
 *
 * ## The XX Pattern
 * ```
 * Initiator (Alice)                 Responder (Bob)
 * â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€                 â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
 * â†’ e                              Generate ephemeral
 * â† e, ee, s, es                   Ephemeral, DH(ee), static encrypted, DH(es)
 * â†’ s, se                          Static encrypted, DH(se)
 * â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â• Transport Keys Established â•â•â•
 * ```
 *
 * After 3 messages, both sides have:
 * - Authenticated each other's static keys
 * - Forward-secret session keys (derived from ephemeral DH)
 * - Separate send/receive cipher states
 *
 * ## Security Properties
 * - **Forward Secrecy**: Ephemeral keys ensure past sessions stay secure
 * - **Identity Hiding**: Static keys encrypted under ephemeral DH
 * - **Mutual Authentication**: Both parties prove possession of static keys
 * - **Key Compromise Impersonation Resistance**: Compromised static key
 *   of A doesn't let attacker impersonate B to A
 *
 * ## Protocol Name
 * `Noise_XX_25519_ChaChaPoly_SHA256`
 */
class NoiseXXHandshake(
    private val localStaticKeyPair: MeshKeyPair,
    private val role: NoiseRole,
    private val keyGen: MeshKeyPairGenerator = MeshKeyPairGenerator(),
    private val cipher: ChaCha20Poly1305Cipher = ChaCha20Poly1305Cipher(),
    private val hkdf: HkdfSha256 = HkdfSha256()
) {
    // Noise protocol state
    private var chainingKey: ByteArray
    private var handshakeHash: ByteArray
    private var localEphemeral: MeshKeyPair? = null
    private var remoteEphemeralPublic: ByteArray? = null
    private var remoteStaticPublic: ByteArray? = null
    private var messageIndex: Int = 0

    // Result keys after handshake completion
    private var sendKey: ByteArray? = null
    private var receiveKey: ByteArray? = null

    /** Whether the handshake has completed successfully. */
    var isComplete: Boolean = false
        private set

    init {
        // Initialize with the protocol name hash
        val protocolName = "Noise_XX_25519_ChaChaPoly_SHA256"
        val protocolNameBytes = protocolName.toByteArray(Charsets.US_ASCII)

        if (protocolNameBytes.size <= 32) {
            // Pad to 32 bytes
            handshakeHash = ByteArray(32)
            System.arraycopy(protocolNameBytes, 0, handshakeHash, 0, protocolNameBytes.size)
        } else {
            handshakeHash = sha256(protocolNameBytes)
        }

        chainingKey = handshakeHash.copyOf()

        // Mix in empty prologue
        mixHash(ByteArray(0))
    }

    /**
     * Process a handshake step. Call this for each message in the XX pattern.
     *
     * @param incomingPayload Received handshake message (null for the first initiator message)
     * @return The handshake message to send to the remote peer
     */
    fun processMessage(incomingPayload: ByteArray? = null): MeshResult<HandshakeOutput> {
        return try {
            when (role) {
                NoiseRole.INITIATOR -> processInitiatorMessage(incomingPayload)
                NoiseRole.RESPONDER -> processResponderMessage(incomingPayload)
            }
        } catch (e: Exception) {
            Timber.e(e, "Noise handshake step $messageIndex failed")
            MeshResult.Error("Handshake failed at step $messageIndex", e)
        }
    }

    // â”€â”€ Initiator Flow â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun processInitiatorMessage(incoming: ByteArray?): MeshResult<HandshakeOutput> {
        return when (messageIndex) {
            0 -> initiatorStep1()  // â†’ e
            1 -> {
                requireNotNull(incoming) { "Initiator step 2 requires responder message" }
                initiatorStep2(incoming)  // â† e, ee, s, es â†’ s, se
            }
            else -> MeshResult.Error("Unexpected initiator step: $messageIndex")
        }
    }

    /**
     * Initiator Step 1: â†’ e
     * Generate ephemeral key pair, send public key.
     */
    private fun initiatorStep1(): MeshResult<HandshakeOutput> {
        val ephResult = keyGen.generateX25519KeyPair()
        val eph = (ephResult as? MeshResult.Success)?.data
            ?: return MeshResult.Error("Failed to generate ephemeral key")

        localEphemeral = eph

        // â†’ e: send ephemeral public key
        val output = eph.publicKey.copyOf()
        mixHash(eph.publicKey)

        messageIndex = 1
        return MeshResult.Success(HandshakeOutput(messageToSend = output))
    }

    /**
     * Initiator Step 2: Process â† e, ee, s, es then send â†’ s, se
     * This is the final step â€” derive transport keys.
     */
    private fun initiatorStep2(message: ByteArray): MeshResult<HandshakeOutput> {
        var offset = 0

        // â† e: read responder ephemeral (32 bytes)
        val remoteEph = message.copyOfRange(offset, offset + 32)
        remoteEphemeralPublic = remoteEph
        mixHash(remoteEph)
        offset += 32

        // ee: DH(local ephemeral, remote ephemeral)
        val localEph = localEphemeral ?: return MeshResult.Error("No local ephemeral")
        val eeSecret = performDH(localEph, remoteEph)
            ?: return MeshResult.Error("DH ee failed")
        mixKey(eeSecret)

        // â† s: decrypt responder static key (32 + 16 tag = 48 bytes)
        val encryptedStatic = message.copyOfRange(offset, offset + 48)
        val decryptResult = decryptAndHash(encryptedStatic)
        val remoteStatic = (decryptResult as? MeshResult.Success)?.data
            ?: return MeshResult.Error("Failed to decrypt responder static key")
        remoteStaticPublic = remoteStatic
        offset += 48

        // es: DH(local ephemeral, remote static)
        val esSecret = performDH(localEph, remoteStatic)
            ?: return MeshResult.Error("DH es failed")
        mixKey(esSecret)

        // Now build our response: â†’ s, se

        // â†’ s: encrypt our static key
        val encryptOurStatic = encryptAndHash(localStaticKeyPair.publicKey)
        val ourEncStatic = (encryptOurStatic as? MeshResult.Success)?.data
            ?: return MeshResult.Error("Failed to encrypt static key")

        // se: DH(local static, remote ephemeral)
        val seSecret = performDH(localStaticKeyPair, remoteEph)
            ?: return MeshResult.Error("DH se failed")
        mixKey(seSecret)

        // Encrypt empty payload to finalize
        val finalPayload = encryptAndHash(ByteArray(0))
        val encPayload = (finalPayload as? MeshResult.Success)?.data
            ?: return MeshResult.Error("Failed to encrypt final payload")

        // Split: derive transport keys
        split()

        val outputMessage = ourEncStatic + encPayload
        messageIndex = 2
        isComplete = true

        return MeshResult.Success(
            HandshakeOutput(
                messageToSend = outputMessage,
                sendCipherKey = sendKey,
                receiveCipherKey = receiveKey,
                remoteStaticKey = remoteStaticPublic
            )
        )
    }

    // â”€â”€ Responder Flow â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun processResponderMessage(incoming: ByteArray?): MeshResult<HandshakeOutput> {
        return when (messageIndex) {
            0 -> {
                requireNotNull(incoming) { "Responder step 1 requires initiator message" }
                responderStep1(incoming)  // â† e â†’ e, ee, s, es
            }
            1 -> {
                requireNotNull(incoming) { "Responder step 2 requires initiator message" }
                responderStep2(incoming)  // â† s, se
            }
            else -> MeshResult.Error("Unexpected responder step: $messageIndex")
        }
    }

    /**
     * Responder Step 1: Read â† e, then send â†’ e, ee, s, es
     */
    private fun responderStep1(message: ByteArray): MeshResult<HandshakeOutput> {
        // â† e: read initiator ephemeral
        val remoteEph = message.copyOfRange(0, 32)
        remoteEphemeralPublic = remoteEph
        mixHash(remoteEph)

        // Generate our ephemeral
        val ephResult = keyGen.generateX25519KeyPair()
        val eph = (ephResult as? MeshResult.Success)?.data
            ?: return MeshResult.Error("Failed to generate ephemeral key")
        localEphemeral = eph

        // â†’ e: send our ephemeral
        val outputParts = mutableListOf<ByteArray>()
        outputParts.add(eph.publicKey)
        mixHash(eph.publicKey)

        // ee: DH(local ephemeral, remote ephemeral)
        val eeSecret = performDH(eph, remoteEph) ?: return MeshResult.Error("DH ee failed")
        mixKey(eeSecret)

        // â†’ s: encrypt our static key
        val encStatic = encryptAndHash(localStaticKeyPair.publicKey)
        val encStaticBytes = (encStatic as? MeshResult.Success)?.data
            ?: return MeshResult.Error("Failed to encrypt static key")
        outputParts.add(encStaticBytes)

        // es: DH(local static, remote ephemeral)
        val esSecret = performDH(localStaticKeyPair, remoteEph)
            ?: return MeshResult.Error("DH es failed")
        mixKey(esSecret)

        // Encrypt empty payload
        val emptyPayload = encryptAndHash(ByteArray(0))
        val encEmpty = (emptyPayload as? MeshResult.Success)?.data
            ?: return MeshResult.Error("Failed to encrypt payload")
        outputParts.add(encEmpty)

        val output = outputParts.fold(ByteArray(0)) { acc, part -> acc + part }

        messageIndex = 1
        return MeshResult.Success(HandshakeOutput(messageToSend = output))
    }

    /**
     * Responder Step 2: Read â† s, se. Derive transport keys.
     */
    private fun responderStep2(message: ByteArray): MeshResult<HandshakeOutput> {
        var offset = 0

        // â† s: decrypt initiator static key (48 bytes = 32 + 16 tag)
        val encryptedStatic = message.copyOfRange(offset, offset + 48)
        val decryptResult = decryptAndHash(encryptedStatic)
        val remoteStatic = (decryptResult as? MeshResult.Success)?.data
            ?: return MeshResult.Error("Failed to decrypt initiator static key")
        remoteStaticPublic = remoteStatic
        offset += 48

        // se: DH(local ephemeral, remote static)
        val localEph = localEphemeral ?: return MeshResult.Error("No local ephemeral")
        val seSecret = performDH(localEph, remoteStatic) ?: return MeshResult.Error("DH se failed")
        mixKey(seSecret)

        // Decrypt payload (should be empty)
        if (offset < message.size) {
            val encPayload = message.copyOfRange(offset, message.size)
            decryptAndHash(encPayload)  // Verify but ignore content
        }

        // Split: derive transport keys
        split()

        messageIndex = 2
        isComplete = true

        return MeshResult.Success(
            HandshakeOutput(
                messageToSend = null,  // No message to send â€” handshake is done
                sendCipherKey = sendKey,
                receiveCipherKey = receiveKey,
                remoteStaticKey = remoteStaticPublic
            )
        )
    }

    // â”€â”€ Noise Internal Operations â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun mixHash(data: ByteArray) {
        handshakeHash = sha256(handshakeHash + data)
    }

    private fun mixKey(inputKeyMaterial: ByteArray) {
        val (newCk, tempK) = hkdf.noiseHkdf(chainingKey, inputKeyMaterial)
        chainingKey = newCk
        // tempK is used for encryption in encryptAndHash/decryptAndHash
        encryptionKey = tempK
        encryptionNonce = 0
    }

    private var encryptionKey: ByteArray? = null
    private var encryptionNonce: Long = 0

    private fun encryptAndHash(plaintext: ByteArray): MeshResult<ByteArray> {
        val key = encryptionKey ?: return MeshResult.Error("No encryption key")
        val result = cipher.encryptWithCounter(key, encryptionNonce, plaintext, handshakeHash)
        encryptionNonce++
        return result.map { ciphertext ->
            mixHash(ciphertext)
            ciphertext
        }
    }

    private fun decryptAndHash(ciphertext: ByteArray): MeshResult<ByteArray> {
        val key = encryptionKey ?: return MeshResult.Error("No encryption key")
        val result = cipher.decryptWithCounter(key, encryptionNonce, ciphertext, handshakeHash)
        encryptionNonce++
        return result.onSuccess { mixHash(ciphertext) }
    }

    private fun performDH(localKeyPair: MeshKeyPair, remotePublicRaw: ByteArray): ByteArray? {
        return try {
            val remotePub = keyGen.x25519PublicKeyFromBytes(remotePublicRaw)
            val result = keyGen.x25519KeyAgreement(localKeyPair.jcaKeyPair.private, remotePub)
            (result as? MeshResult.Success)?.data
        } catch (e: Exception) {
            Timber.e(e, "DH failed")
            null
        }
    }

    /**
     * Split: derive separate send and receive transport keys.
     * Initiator sends with key1, receives with key2.
     * Responder sends with key2, receives with key1.
     */
    private fun split() {
        val (_, key1, key2) = hkdf.noiseHkdf3(chainingKey, ByteArray(0))
        when (role) {
            NoiseRole.INITIATOR -> {
                sendKey = key1
                receiveKey = key2
            }
            NoiseRole.RESPONDER -> {
                sendKey = key2
                receiveKey = key1
            }
        }
        // Clear handshake state
        encryptionKey?.fill(0)
        encryptionKey = null
    }

    private fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }
}

enum class NoiseRole { INITIATOR, RESPONDER }

/**
 * Output of a single handshake step.
 *
 * @param messageToSend Bytes to send to the remote peer (null if no message needed)
 * @param sendCipherKey Derived send key (only set when handshake completes)
 * @param receiveCipherKey Derived receive key (only set when handshake completes)
 * @param remoteStaticKey Remote peer's authenticated static public key
 */
data class HandshakeOutput(
    val messageToSend: ByteArray?,
    val sendCipherKey: ByteArray? = null,
    val receiveCipherKey: ByteArray? = null,
    val remoteStaticKey: ByteArray? = null
)
