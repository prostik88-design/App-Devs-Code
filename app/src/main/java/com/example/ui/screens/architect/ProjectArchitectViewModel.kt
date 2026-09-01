package com.example.ui.screens.architect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DevsCodeRepository
import com.example.domain.model.AiMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArchitectUiState(
    val selectedPattern: String = "Clean Architecture (MVI)",
    val selectedArchitectureType: String = "Android Modular",
    val appScopeDescription: String = "",
    val isGenerating: Boolean = false,
    val generatedArchitectureResult: String = "",
    val error: String? = null
)

class ProjectArchitectViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = UserPreferencesRepository(application)
    private val devsCodeRepository = DevsCodeRepository.getInstance(application)

    private val _uiState = MutableStateFlow(ArchitectUiState())
    val uiState: StateFlow<ArchitectUiState> = _uiState.asStateFlow()

    val allProjects = devsCodeRepository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPattern(pattern: String) {
        _uiState.value = _uiState.value.copy(selectedPattern = pattern)
    }

    fun setScopeDescription(desc: String) {
        _uiState.value = _uiState.value.copy(appScopeDescription = desc)
    }

    fun generateArchitectureBlueprint(targetProjectId: Long? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.appScopeDescription.isBlank()) return@launch

            _uiState.value = state.copy(isGenerating = true, error = null)

            val customApiKey = preferencesRepository.customApiKey.first()
            val selectedModel = preferencesRepository.selectedModel.first()

            val prompt = """
Ты — Principal Mobile Architect & Staff Android Engineer в Devs Code Studio.
Пользователь запрашивает проектирование полноценной архитектуры мобильного приложения.

Параметры задачи:
- Паттерн: ${state.selectedPattern}
- Описание приложения: ${state.appScopeDescription}

Создай подробную архитектурную спецификацию:
1. Архитектурная диаграмма в виде ASCII-схемы (Data Flow, MVI/MVVM State loop).
2. Модульная структура проекта (Gradle modules: `:app`, `:core:model`, `:core:data`, `:core:network`, `:core:database`, `:core:designsystem`, `:feature:...`).
3. Спецификация слоев:
   - Domain Layer (Entities, UseCases/Interactors, Repository Interfaces)
   - Data Layer (Local Room DB, Remote Retrofit/Ktor, Repository Impl, Mappers)
   - Presentation Layer (StateFlow, UI State, UI Event, Jetpack Compose Screens)
4. Полные примеры кода ключевых классов (UiState, ViewModel с MVI reducer, UseCase, Repository).
5. Стратегия тестирования и обработки ошибок.
""".trimIndent()

            try {
                val response = devsCodeRepository.generateAiResponse(
                    chatId = -1, // Use -1 or handle appropriately if generating without chat context
                    userPrompt = prompt,
                    mode = AiMode.APP_GENERATOR,
                    projectId = targetProjectId,
                    attachedCodeOrFile = null,
                    customKey = customApiKey,
                    modelName = selectedModel
                )
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    generatedArchitectureResult = response
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = e.localizedMessage ?: "Не удалось спроектировать архитектуру"
                )
            }
        }
    }
}
