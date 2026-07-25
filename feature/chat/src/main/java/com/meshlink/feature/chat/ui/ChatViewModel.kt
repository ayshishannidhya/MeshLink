package com.meshlink.feature.chat.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshlink.core.database.dao.ConversationDao
import com.meshlink.core.database.dao.MessageDao
import com.meshlink.core.database.dao.PeerDao
import com.meshlink.core.database.entity.ConversationEntity
import com.meshlink.core.database.entity.MessageEntity
import com.meshlink.core.database.entity.PeerEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageDao: MessageDao,
    private val peerDao: PeerDao,
    private val conversationDao: ConversationDao
) : ViewModel() {
    
    val peerId: String = savedStateHandle.get<String>("peerId") ?: ""
    
    val messages: StateFlow<List<MessageEntity>> = messageDao
        .getMessagesForConversation(peerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val _peer = MutableStateFlow<PeerEntity?>(null)
    val peer: StateFlow<PeerEntity?> = _peer.asStateFlow()
    
    init {
        viewModelScope.launch {
            _peer.value = peerDao.getPeerById(peerId)
            // Clear unread when opening chat
            conversationDao.clearUnread(peerId)
        }
    }
    
    fun sendMessage(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val msgId = UUID.randomUUID().toString()
            val msg = MessageEntity(
                id = msgId,
                conversationId = peerId,
                senderId = "local",
                recipientId = peerId,
                encryptedContent = content.toByteArray(Charsets.UTF_8),
                timestamp = System.currentTimeMillis(),
                status = 0, // queued
                hopCount = 0
            )
            messageDao.insertMessage(msg)
            
            // Update or create conversation
            val existing = conversationDao.getConversationById(peerId)
            if (existing != null) {
                conversationDao.updateConversation(existing.copy(
                    lastMessagePreview = content,
                    lastMessageTimestamp = System.currentTimeMillis()
                ))
            } else {
                conversationDao.insertConversation(ConversationEntity(
                    id = peerId,
                    type = 0,
                    title = _peer.value?.displayName ?: "Peer ${peerId.take(8)}",
                    participantIds = peerId,
                    lastMessagePreview = content,
                    lastMessageTimestamp = System.currentTimeMillis()
                ))
            }
        }
    }
}
