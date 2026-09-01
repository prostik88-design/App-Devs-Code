package com.example.ui.screens.generator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DevsCodeRepository
import com.example.domain.model.AiMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GeneratorUiState(
    val prompt: String = "",
    val isAnalyzing: Boolean = false,
    val analysisResult: String? = null,
    val generatedProjectId: Long? = null,
    val generatedProjectName: String? = null,
    val isProjectCreated: Boolean = false,
    val errorMessage: String? = null
)

class AppGeneratorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DevsCodeRepository.getInstance(application)
    private val preferencesRepository = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    private var customApiKey: String = ""
    private var selectedModel: String = "gemini-3.5-flash"

    init {
        viewModelScope.launch {
            preferencesRepository.customApiKey.collect { customApiKey = it }
        }
        viewModelScope.launch {
            preferencesRepository.selectedModel.collect { selectedModel = it }
        }
    }

    fun setPrompt(text: String) {
        _uiState.value = _uiState.value.copy(prompt = text)
    }

    fun analyzeAndGenerate(customPrompt: String? = null) {
        val targetPrompt = customPrompt ?: _uiState.value.prompt
        if (targetPrompt.isBlank()) return

        _uiState.value = _uiState.value.copy(
            prompt = targetPrompt,
            isAnalyzing = true,
            analysisResult = null,
            isProjectCreated = false,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                // Request comprehensive architecture blueprint
                val promptBlueprint = buildString {
                    append("Создай детальный архитектурный проект и спецификацию для следующего приложения:\n")
                    append("«$targetPrompt»\n\n")
                    append("Определи структурированно:\n")
                    append("1. Название приложения\n")
                    append("2. Платформа (Android Native)\n")
                    append("3. Язык и Framework (Kotlin 2.2, Jetpack Compose Material 3)\n")
                    append("4. Архитектура (MVVM, Clean Architecture, Repository Pattern)\n")
                    append("5. Экраны и UI-компоненты\n")
                    append("6. Зависимости (Gradle Kotlin DSL)\n")
                    append("7. Модель данных и структура Room Database (Entity, DAO, Database)\n")
                    append("8. Необходимые API и разрешения AndroidManifest.xml\n")
                    append("9. Порядок реализации (Roadmap)\n")
                    append("10. Полная файловая структура проекта\n")
                }

                val aiResult = repository.generateAiResponse(
                    chatId = 1L,
                    userPrompt = promptBlueprint,
                    mode = AiMode.APP_GENERATOR,
                    customKey = customApiKey,
                    modelName = selectedModel
                )

                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    analysisResult = aiResult
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = "Ошибка генерации: ${e.localizedMessage}"
                )
            }
        }
    }

    fun buildAndSaveProject(title: String, description: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            try {
                val projectId = repository.generateCompleteProjectFromBlueprint(
                    title = title.ifBlank { "DevsCodeApp" },
                    description = description.ifBlank { _uiState.value.prompt }
                )
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    generatedProjectId = projectId,
                    generatedProjectName = title,
                    isProjectCreated = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = "Не удалось сохранить проект: ${e.localizedMessage}"
                )
            }
        }
    }
}
