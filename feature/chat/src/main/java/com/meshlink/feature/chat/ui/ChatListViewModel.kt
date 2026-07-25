package com.meshlink.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.core.database.dao.ConversationDao
import com.meshlink.core.domain.repository.MeshRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val conversationDao: ConversationDao,
    private val meshRepository: MeshRepository
) : ViewModel() {

    val uiState: StateFlow<ChatListUiState> = combine(
        conversationDao.getAllConversations(),
        meshRepository.activePeers,
        meshRepository.isMeshActive
    ) { conversations, peers, meshActive ->
        ChatListUiState(
            conversations = conversations.map { conv ->
                ConversationUiModel(
                    id = conv.id,
                    title = conv.title ?: "Peer ${conv.id.take(8)}",
                    lastMessage = conv.lastMessagePreview ?: "No messages yet",
                    timeAgo = formatTimeAgo(conv.lastMessageTimestamp),
                    unreadCount = conv.unreadCount,
                    isOnline = false, // Will be true when real-time connectivity is added
                    avatarLetter = (conv.title ?: "P").take(1).uppercase()
                )
            },
            isMeshActive = meshActive,
            peerCount = peers.size,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatListUiState())

    private fun formatTimeAgo(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "now"
            diff < 3_600_000 -> "${diff / 60_000}m"
            diff < 86_400_000 -> "${diff / 3_600_000}h"
            else -> "${diff / 86_400_000}d"
        }
    }
}
