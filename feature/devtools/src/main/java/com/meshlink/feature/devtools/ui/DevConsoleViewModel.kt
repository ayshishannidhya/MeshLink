package com.meshlink.feature.devtools.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.core.common.toShortHex
import com.meshlink.core.mesh.MeshEngine
import com.meshlink.core.mesh.MeshStats
import com.meshlink.core.mesh.routing.NeighborEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DevConsoleViewModel @Inject constructor(
    private val meshEngine: MeshEngine
) : ViewModel() {

    val meshStats: StateFlow<MeshStats> = meshEngine.meshStats

    val activePeers: StateFlow<List<NeighborEntry>> = meshEngine.neighbors.activePeersFlow

    private val _packetLog = MutableStateFlow<List<String>>(emptyList())
    val packetLog: StateFlow<List<String>> = _packetLog.asStateFlow()

    init {
        viewModelScope.launch {
            meshEngine.incomingMessages.collect { packet ->
                val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
                val senderId = packet.senderId.toShortHex()
                val entry = "$timestamp RX ttl=${packet.ttl} from=$senderId"
                _packetLog.update { (it + entry).takeLast(200) }
            }
        }
    }
}
