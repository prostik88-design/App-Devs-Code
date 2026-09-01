package com.example.ui.screens.chat

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DevsCodeRepository
import com.example.domain.model.AiMode
import com.example.domain.model.AiNetworkState
import com.example.domain.model.ApiErrorType
import com.example.util.NetworkUtils
import com.example.util.SafFileManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val activeChatId: Long? = null,
    val currentChat: ChatEntity? = null,
    val messages: List<MessageEntity> = emptyList(),
    val selectedMode: AiMode = AiMode.CREATION,
    val selectedModel: String = "gemini-3.5-flash",
    val selectedProjectId: Long? = null,
    val projectFiles: List<com.example.data.local.entity.ProjectFileEntity> = emptyList(),
    val attachedCode: String? = null,
    val attachedFileName: String? = null,
    val attachedFileSize: String? = null,
    val isLoading: Boolean = false,
    val generationState: AiNetworkState = AiNetworkState.Idle,
    val lastFailedPrompt: String? = null,
    val lastFailedAttachedCode: String? = null,
    val lastFailedAttachedFileName: String? = null,
    val errorMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DevsCodeRepository.getInstance(application)
    private val preferencesRepository = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val allProjects: StateFlow<List<ProjectEntity>> = repository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChats: StateFlow<List<ChatEntity>> = repository.getAllChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var activeJob: Job? = null
    private var customApiKey: String = ""

    init {
        viewModelScope.launch {
            preferencesRepository.selectedModel.collect { model ->
                _uiState.value = _uiState.value.copy(selectedModel = model)
            }
        }
        viewModelScope.launch {
            preferencesRepository.customApiKey.collect { key ->
                customApiKey = key
            }
        }
        viewModelScope.launch {
            preferencesRepository.activeProjectId.collect { projId ->
                _uiState.value = _uiState.value.copy(selectedProjectId = projId)
                loadProjectFiles(projId)
            }
        }
        ensureActiveChat()
    }

    private fun ensureActiveChat() {
        viewModelScope.launch {
            allChats.collect { chats ->
                if (_uiState.value.activeChatId == null) {
                    if (chats.isNotEmpty()) {
                        selectChat(chats.first().id)
                    } else {
                        val newId = repository.createChat(title = "Devs Code AI Assistant")
                        selectChat(newId)
                    }
                }
            }
        }
    }

    fun selectChat(chatId: Long) {
        _uiState.value = _uiState.value.copy(activeChatId = chatId)
        viewModelScope.launch {
            val chat = repository.getChatById(chatId)
            _uiState.value = _uiState.value.copy(currentChat = chat)
            repository.getMessagesForChat(chatId).collect { msgs ->
                _uiState.value = _uiState.value.copy(messages = msgs)
            }
        }
    }

    fun createNewChat(projectId: Long? = null, title: String = "Новый диалог") {
        viewModelScope.launch {
            val newId = repository.createChat(projectId = projectId, title = title)
            selectChat(newId)
        }
    }

    fun setMode(mode: AiMode) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun setModel(model: String) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
        viewModelScope.launch {
            preferencesRepository.setSelectedModel(model)
        }
    }

    private var projectFilesJob: Job? = null

    fun setProject(projectId: Long?) {
        _uiState.value = _uiState.value.copy(selectedProjectId = projectId)
        viewModelScope.launch {
            preferencesRepository.setActiveProjectId(projectId)
        }
        loadProjectFiles(projectId)
    }

    private fun loadProjectFiles(projectId: Long?) {
        projectFilesJob?.cancel()
        if (projectId == null) {
            _uiState.value = _uiState.value.copy(projectFiles = emptyList())
            return
        }
        projectFilesJob = viewModelScope.launch {
            repository.getFilesForProject(projectId).collect { files ->
                _uiState.value = _uiState.value.copy(projectFiles = files)
            }
        }
    }

    fun attachCode(fileName: String, code: String, sizeStr: String? = null) {
        _uiState.value = _uiState.value.copy(
            attachedFileName = fileName,
            attachedCode = code,
            attachedFileSize = sizeStr ?: SafFileManager.formatFileSize(code.length.toLong())
        )
    }

    fun attachSafFileUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = SafFileManager.readSingleFile(context, uri)
            result.onSuccess { imported ->
                attachCode(imported.name, imported.content, imported.formattedSize)
                Toast.makeText(context, "Файл «${imported.name}» прикреплен (${imported.formattedSize})", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, "Ошибка чтения файла: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun clearAttachment() {
        _uiState.value = _uiState.value.copy(
            attachedFileName = null,
            attachedCode = null,
            attachedFileSize = null
        )
    }

    fun sendMessage(userText: String) {
        val chatId = _uiState.value.activeChatId ?: return
        if (userText.isBlank() && _uiState.value.attachedCode.isNullOrBlank()) return

        val attachedCode = _uiState.value.attachedCode
        val attachedFile = _uiState.value.attachedFileName
        val mode = _uiState.value.selectedMode
        val model = _uiState.value.selectedModel
        val projectId = _uiState.value.selectedProjectId

        // Clear attachment from input box
        clearAttachment()

        val fullMessageContent = buildString {
            if (!attachedFile.isNullOrBlank() && !attachedCode.isNullOrBlank()) {
                append("📁 **Прикрепленный файл:** `$attachedFile`\n```\n$attachedCode\n```\n\n")
            }
            append(userText)
        }

        viewModelScope.launch {
            // 1. Insert user message
            repository.insertMessage(
                MessageEntity(
                    chatId = chatId,
                    role = "user",
                    content = fullMessageContent,
                    modelName = model
                )
            )

            // Auto-rename chat title if it's the default
            val currentChat = _uiState.value.currentChat
            if (currentChat != null && currentChat.title.startsWith("Новый диалог")) {
                val shortTitle = userText.take(28).trim().ifBlank { "Диалог с AI" }
                repository.updateChat(currentChat.copy(title = shortTitle))
            }

            // Save last prompt for retry
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                generationState = AiNetworkState.Loading,
                lastFailedPrompt = userText,
                lastFailedAttachedCode = attachedCode,
                lastFailedAttachedFileName = attachedFile,
                errorMessage = null
            )

            executeGenerationInternal(
                chatId = chatId,
                userText = userText,
                mode = mode,
                model = model,
                projectId = projectId,
                attachedCode = attachedCode
            )
        }
    }

    fun retryLastMessage() {
        val lastPrompt = _uiState.value.lastFailedPrompt ?: return
        val chatId = _uiState.value.activeChatId ?: return
        val attachedCode = _uiState.value.lastFailedAttachedCode
        val mode = _uiState.value.selectedMode
        val model = _uiState.value.selectedModel
        val projectId = _uiState.value.selectedProjectId

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            generationState = AiNetworkState.Loading,
            errorMessage = null
        )

        viewModelScope.launch {
            executeGenerationInternal(
                chatId = chatId,
                userText = lastPrompt,
                mode = mode,
                model = model,
                projectId = projectId,
                attachedCode = attachedCode
            )
        }
    }

    private suspend fun executeGenerationInternal(
        chatId: Long,
        userText: String,
        mode: AiMode,
        model: String,
        projectId: Long?,
        attachedCode: String?
    ) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    generationState = AiNetworkState.Loading
                )

                val response = repository.generateAiResponse(
                    chatId = chatId,
                    userPrompt = userText,
                    mode = mode,
                    projectId = projectId,
                    attachedCodeOrFile = attachedCode,
                    customKey = customApiKey,
                    modelName = model
                )

                if (response.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generationState = AiNetworkState.EmptyData,
                        errorMessage = "Ответ от AI пуст. Попробуйте уточнить запрос."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generationState = AiNetworkState.Success(response),
                        lastFailedPrompt = null
                    )

                    repository.insertMessage(
                        MessageEntity(
                            chatId = chatId,
                            role = "assistant",
                            content = response,
                            modelName = model
                        )
                    )
                }
            } catch (e: CancellationException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generationState = AiNetworkState.Cancelled,
                    errorMessage = "Генерация отменена пользователем."
                )
            } catch (e: Exception) {
                val networkState = NetworkUtils.mapExceptionToAiNetworkState(e)
                val errorMsg = when (networkState) {
                    is AiNetworkState.NoConnection -> networkState.message
                    is AiNetworkState.Timeout -> networkState.message
                    is AiNetworkState.ApiError -> "${networkState.errorType.userTitleRu}: ${networkState.message}"
                    is AiNetworkState.ServerError -> networkState.message
                    else -> e.localizedMessage ?: "Сетевая ошибка при обращении к AI"
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generationState = networkState,
                    errorMessage = errorMsg
                )

                repository.insertMessage(
                    MessageEntity(
                        chatId = chatId,
                        role = "assistant",
                        content = "⚠️ **$errorMsg**\n\nНажмите кнопку «Повторить» ниже или проверьте параметры подключения в Настройках.",
                        modelName = model,
                        isError = true
                    )
                )
            }
        }
    }

    fun stopGeneration() {
        activeJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            generationState = AiNetworkState.Cancelled
        )
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun clearChat() {
        val chatId = _uiState.value.activeChatId ?: return
        viewModelScope.launch {
            repository.clearChatMessages(chatId)
        }
    }
}
