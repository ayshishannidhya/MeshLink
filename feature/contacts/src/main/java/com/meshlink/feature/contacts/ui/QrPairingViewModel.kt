package com.meshlink.feature.contacts.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.core.common.MeshPreferences
import com.meshlink.core.database.dao.PeerDao
import com.meshlink.core.database.entity.PeerEntity
import com.meshlink.core.domain.repository.IdentityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrPairingViewModel @Inject constructor(
    private val identityRepository: IdentityRepository,
    private val peerDao: PeerDao,
    private val meshPreferences: MeshPreferences
) : ViewModel() {

    // Local identity for QR generation
    val localPublicKeyHex: String by lazy { identityRepository.getPublicKeyHex() }
    val localFingerprint: String by lazy { identityRepository.getFingerprint() }
    
    // Display name flow
    val displayName: StateFlow<String> = meshPreferences.displayName
        .stateIn(viewModelScope, SharingStarted.Eagerly, "MeshLink User")

    private val _pairingResult = MutableSharedFlow<PairingResult>()
    val pairingResult: SharedFlow<PairingResult> = _pairingResult.asSharedFlow()

    /**
     * Generates the QR data string containing this device's identity.
     */
    fun getQrData(): String {
        val name = displayName.value
        return "meshlink://pair?pk=${localPublicKeyHex}&name=${Uri.encode(name)}"
    }

    /**
     * Called when a QR code is scanned. Parses the data and saves the peer.
     */
    fun onQrScanned(rawData: String) {
        viewModelScope.launch {
            try {
                val uri = Uri.parse(rawData)
                if (uri.scheme != "meshlink" || uri.host != "pair") {
                    _pairingResult.emit(PairingResult.Error("Invalid QR code"))
                    return@launch
                }

                val publicKeyHex = uri.getQueryParameter("pk")
                val peerName = uri.getQueryParameter("name") ?: "Unknown Peer"

                if (publicKeyHex.isNullOrBlank()) {
                    _pairingResult.emit(PairingResult.Error("Missing public key in QR code"))
                    return@launch
                }

                // Check if already paired
                val existing = peerDao.getPeerByPublicKey(publicKeyHex)
                if (existing != null) {
                    _pairingResult.emit(PairingResult.AlreadyPaired(existing.displayName))
                    return@launch
                }

                // Generate a unique peer ID from public key
                val peerId = publicKeyHex.take(16) // First 8 bytes as hex

                val peer = PeerEntity(
                    id = peerId,
                    publicKeyHex = publicKeyHex,
                    signingKeyHex = publicKeyHex, // Same key for now
                    displayName = peerName,
                    lastSeen = System.currentTimeMillis(),
                    trustScore = 0.8f, // QR-verified peers get higher trust
                    createdAt = System.currentTimeMillis()
                )

                peerDao.insertPeer(peer)
                _pairingResult.emit(PairingResult.Success(peerName))
            } catch (e: Exception) {
                _pairingResult.emit(PairingResult.Error("Failed to process QR: ${e.message}"))
            }
        }
    }
}

sealed class PairingResult {
    data class Success(val peerName: String) : PairingResult()
    data class AlreadyPaired(val peerName: String) : PairingResult()
    data class Error(val message: String) : PairingResult()
}
