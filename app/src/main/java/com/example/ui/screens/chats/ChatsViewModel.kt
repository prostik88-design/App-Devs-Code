package com.example.ui.screens.chats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.repository.DevsCodeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatsUiState(
    val chats: List<ChatEntity> = emptyList(),
    val filteredChats: List<ChatEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedChat: ChatEntity? = null,
    val currentMessages: List<MessageEntity> = emptyList()
)

class ChatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DevsCodeRepository.getInstance(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _selectedChat = MutableStateFlow<ChatEntity?>(null)
    val selectedChat = _selectedChat.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val currentMessages = _currentMessages.asStateFlow()

    val allChats: StateFlow<List<ChatEntity>> = repository.getAllChatSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ChatsUiState> = combine(
        allChats,
        _searchQuery,
        _isLoading,
        _selectedChat,
        _currentMessages
    ) { chats, query, loading, selected, messages ->
        val filtered = if (query.isBlank()) {
            chats
        } else {
            chats.filter { it.title.contains(query, ignoreCase = true) }
        }
        ChatsUiState(
            chats = chats,
            filteredChats = filtered,
            searchQuery = query,
            isLoading = loading,
            selectedChat = selected,
            currentMessages = messages
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatsUiState())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createNewChat(title: String = "Новый диалог", projectId: Long? = null, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = repository.createNewChatSession(projectId, title)
            onCreated(newId)
        }
    }

    fun loadChatDetails(chatId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            allChats.value.find { it.id == chatId }?.let {
                _selectedChat.value = it
            }
            repository.getMessagesForChat(chatId).collect { msgs ->
                _currentMessages.value = msgs
                _isLoading.value = false
            }
        }
    }

    fun renameChat(chatId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.renameChatSession(chatId, newTitle)
        }
    }

    fun deleteChat(chatId: Long) {
        viewModelScope.launch {
            repository.deleteChatSession(chatId)
            if (_selectedChat.value?.id == chatId) {
                _selectedChat.value = null
                _currentMessages.value = emptyList()
            }
        }
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }
}
