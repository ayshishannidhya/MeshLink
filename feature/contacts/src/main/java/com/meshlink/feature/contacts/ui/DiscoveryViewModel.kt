package com.meshlink.feature.contacts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.core.database.dao.PeerDao
import com.meshlink.core.database.entity.PeerEntity
import com.meshlink.core.domain.repository.MeshRepository
import com.meshlink.core.mesh.routing.NeighborEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val peerDao: PeerDao
) : ViewModel() {

    val discoveredPeers: StateFlow<List<NeighborEntry>> = meshRepository.activePeers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun savePeer(entry: NeighborEntry) {
        viewModelScope.launch {
            val peerIdHex = entry.peerId.joinToString("") { "%02X".format(it) }
            val peer = PeerEntity(
                id = peerIdHex.take(16),
                publicKeyHex = peerIdHex,
                signingKeyHex = peerIdHex,
                displayName = entry.displayName ?: "Peer ${peerIdHex.take(8)}",
                lastSeen = System.currentTimeMillis(),
                trustScore = 0.5f,
                createdAt = System.currentTimeMillis()
            )
            peerDao.insertPeer(peer)
        }
    }
}
