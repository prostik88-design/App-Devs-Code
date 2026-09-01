package com.example.ui.screens.editor

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ProjectFileEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DevsCodeRepository
import com.example.domain.model.AiMode
import com.example.util.SafFileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditorUiState(
    val currentFileId: Long? = null,
    val currentFile: ProjectFileEntity? = null,
    val projectFiles: List<ProjectFileEntity> = emptyList(),
    val content: String = "",
    val originalContent: String = "",
    val proposedAiContent: String? = null,
    val isAiProcessing: Boolean = false,
    val aiExplanation: String? = null,
    val isSaved: Boolean = true,
    val showLineNumbers: Boolean = true,
    val wordWrap: Boolean = false,
    val fontSize: Int = 14,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val isSearchVisible: Boolean = false
)

enum class CodeAction(val title: String, val prompt: String) {
    EXPLAIN("Объяснить код", "Подробно объясни логику, архитектуру и ключевые функции этого кода:"),
    FIX("Исправить ошибки", "Найди все синтаксические, логические ошибки и потенциальные NullPointer/краши и покажи исправленный код:"),
    OPTIMIZE("Оптимизировать", "Оптимизируй производительность, расход памяти и убери лишние вызовы в этом коде:"),
    REFACTOR("Рефакторинг", "Проведи чистый рефакторинг с соблюдением Clean Architecture, SOLID и разделения ответственности:"),
    ADD_COMMENTS("Добавить комментарии", "Добавь подробные KDoc/Javadoc комментарии и объяснения к сложным участкам кода:"),
    TESTS("Создать Unit-тесты", "Напиши исчерпывающий набор Unit-тестов (JUnit 5 + MockK / Robolectric) для тестирования этого кода:"),
    AUDIT("Аудит багов и уязвимостей", "Проведи аудит безопасности, утечек памяти, потокобезопасности и уязвимостей:")
}

class CodeEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DevsCodeRepository.getInstance(application)
    private val preferencesRepository = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    private var customApiKey: String = ""
    private var selectedModel: String = "gemini-3.5-flash"

    init {
        viewModelScope.launch {
            preferencesRepository.editorLineNumbers.collect {
                _uiState.value = _uiState.value.copy(showLineNumbers = it)
            }
        }
        viewModelScope.launch {
            preferencesRepository.editorWordWrap.collect {
                _uiState.value = _uiState.value.copy(wordWrap = it)
            }
        }
        viewModelScope.launch {
            preferencesRepository.editorFontSize.collect {
                _uiState.value = _uiState.value.copy(fontSize = it)
            }
        }
        viewModelScope.launch {
            preferencesRepository.customApiKey.collect { customApiKey = it }
        }
        viewModelScope.launch {
            preferencesRepository.selectedModel.collect { selectedModel = it }
        }
    }

    fun loadFile(fileId: Long, projectId: Long? = null) {
        viewModelScope.launch {
            val file = repository.getFileById(fileId)
            if (file != null) {
                _uiState.value = _uiState.value.copy(
                    currentFileId = file.id,
                    currentFile = file,
                    content = file.content,
                    originalContent = file.content,
                    isSaved = true,
                    proposedAiContent = null,
                    aiExplanation = null
                )
                undoStack.clear()
                redoStack.clear()
                undoStack.add(file.content)
            }

            val targetProjectId = projectId ?: file?.projectId
            if (targetProjectId != null) {
                repository.getFilesForProject(targetProjectId).collect { files ->
                    _uiState.value = _uiState.value.copy(projectFiles = files)
                }
            }
        }
    }

    fun openExternalSafUri(context: Context, uri: Uri, projectId: Long = 1L) {
        viewModelScope.launch {
            val result = SafFileManager.readSingleFile(context, uri)
            val imported = result.getOrNull()
            if (imported != null) {
                val fileId = repository.importSafFile(
                    projectId = projectId,
                    name = imported.name,
                    relativePath = imported.relativePath,
                    language = imported.extension,
                    content = imported.content,
                    size = imported.size
                )
                loadFile(fileId, projectId)
            }
        }
    }

    fun renameCurrentFile(newName: String, newRelPath: String) {
        val fileId = _uiState.value.currentFileId ?: return
        viewModelScope.launch {
            repository.renameFile(fileId, newName, newRelPath)
            val updated = repository.getFileById(fileId)
            if (updated != null) {
                _uiState.value = _uiState.value.copy(currentFile = updated)
            }
        }
    }

    fun onContentChanged(newContent: String) {
        if (_uiState.value.content != newContent) {
            undoStack.add(_uiState.value.content)
            redoStack.clear()
            _uiState.value = _uiState.value.copy(
                content = newContent,
                isSaved = newContent == _uiState.value.originalContent
            )
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val last = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_uiState.value.content)
            _uiState.value = _uiState.value.copy(
                content = last,
                isSaved = last == _uiState.value.originalContent
            )
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_uiState.value.content)
            _uiState.value = _uiState.value.copy(
                content = next,
                isSaved = next == _uiState.value.originalContent
            )
        }
    }

    fun saveCurrentFile() {
        val fileId = _uiState.value.currentFileId ?: return
        viewModelScope.launch {
            repository.saveFile(fileId, _uiState.value.content)
            val updated = repository.getFileById(fileId)
            _uiState.value = _uiState.value.copy(
                currentFile = updated,
                originalContent = _uiState.value.content,
                isSaved = true
            )
        }
    }

    fun toggleLineNumbers() {
        val next = !_uiState.value.showLineNumbers
        _uiState.value = _uiState.value.copy(showLineNumbers = next)
        viewModelScope.launch { preferencesRepository.setEditorLineNumbers(next) }
    }

    fun toggleWordWrap() {
        val next = !_uiState.value.wordWrap
        _uiState.value = _uiState.value.copy(wordWrap = next)
        viewModelScope.launch { preferencesRepository.setEditorWordWrap(next) }
    }

    fun setFontSize(size: Int) {
        _uiState.value = _uiState.value.copy(fontSize = size)
        viewModelScope.launch { preferencesRepository.setEditorFontSize(size) }
    }

    fun toggleSearch() {
        _uiState.value = _uiState.value.copy(isSearchVisible = !_uiState.value.isSearchVisible)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setReplaceQuery(query: String) {
        _uiState.value = _uiState.value.copy(replaceQuery = query)
    }

    fun replaceAll() {
        val q = _uiState.value.searchQuery
        val r = _uiState.value.replaceQuery
        if (q.isNotEmpty()) {
            val replaced = _uiState.value.content.replace(q, r)
            onContentChanged(replaced)
        }
    }

    fun executeAiAction(action: CodeAction, customPrompt: String? = null) {
        val currentCode = _uiState.value.content
        if (currentCode.isBlank()) return

        _uiState.value = _uiState.value.copy(isAiProcessing = true, proposedAiContent = null, aiExplanation = null)

        viewModelScope.launch {
            try {
                val fullPrompt = buildString {
                    append(action.prompt)
                    if (!customPrompt.isNullOrBlank()) {
                        append("\nДополнительные указания: $customPrompt")
                    }
                    append("\n\nИсходный код файла (${_uiState.value.currentFile?.name ?: "code"}):\n")
                    append("```${_uiState.value.currentFile?.language ?: "kotlin"}\n")
                    append(currentCode)
                    append("\n```\n")
                    append("Если требуется изменить код, обязательно выдели итоговый полный исправленный код в отдельный блок ```...```, а также дай пояснения.")
                }

                val aiResult = repository.generateAiResponse(
                    chatId = 1L,
                    userPrompt = fullPrompt,
                    mode = AiMode.FIXING,
                    projectId = _uiState.value.currentFile?.projectId,
                    customKey = customApiKey,
                    modelName = selectedModel
                )

                // Extract code from response if any
                val extractedCode = extractCodeBlock(aiResult)

                _uiState.value = _uiState.value.copy(
                    isAiProcessing = false,
                    aiExplanation = aiResult,
                    proposedAiContent = extractedCode
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAiProcessing = false,
                    aiExplanation = "Ошибка: ${e.localizedMessage}"
                )
            }
        }
    }

    fun acceptProposedChanges() {
        val proposed = _uiState.value.proposedAiContent ?: return
        onContentChanged(proposed)
        _uiState.value = _uiState.value.copy(proposedAiContent = null, aiExplanation = null)
    }

    fun rejectProposedChanges() {
        _uiState.value = _uiState.value.copy(proposedAiContent = null, aiExplanation = null)
    }

    private fun extractCodeBlock(response: String): String? {
        val match = Regex("```(?:[a-zA-Z0-9_-]+)?\\n([\\s\\S]*?)```").find(response)
        return match?.groupValues?.get(1)?.trim()
    }
}
