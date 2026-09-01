package com.example.ui.screens.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CodeReviewEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DevsCodeRepository
import com.example.domain.model.AiMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CodeReviewUiState(
    val codeInput: String = "",
    val isReviewing: Boolean = false,
    val reviewMarkdown: String? = null,
    val reviewsList: List<CodeReviewEntity> = emptyList(),
    val selectedSeverityTab: String = "ALL" // "ALL", "CRITICAL", "WARNING", "IMPROVEMENT", "GOOD"
)

class CodeReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DevsCodeRepository.getInstance(application)
    private val preferencesRepository = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(CodeReviewUiState())
    val uiState: StateFlow<CodeReviewUiState> = _uiState.asStateFlow()

    private var customApiKey: String = ""
    private var selectedModel: String = "gemini-3.5-flash"

    init {
        viewModelScope.launch {
            preferencesRepository.customApiKey.collect { customApiKey = it }
        }
        viewModelScope.launch {
            preferencesRepository.selectedModel.collect { selectedModel = it }
        }
        viewModelScope.launch {
            repository.getAllReviews().collect { reviews ->
                _uiState.value = _uiState.value.copy(reviewsList = reviews)
            }
        }
    }

    fun setCodeInput(code: String) {
        _uiState.value = _uiState.value.copy(codeInput = code)
    }

    fun setSeverityTab(tab: String) {
        _uiState.value = _uiState.value.copy(selectedSeverityTab = tab)
    }

    fun runCodeReview(customCode: String? = null) {
        val targetCode = customCode ?: _uiState.value.codeInput
        if (targetCode.isBlank()) return

        _uiState.value = _uiState.value.copy(isReviewing = true, reviewMarkdown = null)

        viewModelScope.launch {
            try {
                val prompt = buildString {
                    append("Проведи подробный Code Review следующего фрагмента кода.\n")
                    append("Раздели анализ строго на 4 блока:\n\n")
                    append("1. 🔴 КРИТИЧЕСКИЕ ПРОБЛЕМЫ (Баги, утечки памяти, краши, уязвимости)\n")
                    append("2. 🟡 ПРЕДУПРЕЖДЕНИЯ (Потенциальные ошибки, неоптимальные вызовы, нарушение стандартов)\n")
                    append("3. 🔵 МОЖНО УЛУЧШИТЬ (Читаемость, Clean Architecture, SOLID, рефакторинг)\n")
                    append("4. 🟢 ХОРОШИЕ РЕШЕНИЯ (Что сделано правильно и эффективно)\n\n")
                    append("В конце обязательно приведи ПОЛНЫЙ ИСПРАВЛЕННЫЙ КОД в блоке ```...\n\n")
                    append("Исходный код:\n```\n$targetCode\n```")
                }

                val aiResult = repository.generateAiResponse(
                    chatId = 1L,
                    userPrompt = prompt,
                    mode = AiMode.ANALYSIS,
                    customKey = customApiKey,
                    modelName = selectedModel
                )

                _uiState.value = _uiState.value.copy(
                    isReviewing = false,
                    reviewMarkdown = aiResult
                )

                // Save review record to Room
                repository.insertReview(
                    CodeReviewEntity(
                        severity = "CRITICAL",
                        title = "Code Review: " + targetCode.take(30).replace("\n", " "),
                        description = aiResult,
                        suggestion = "Изучите рекомендации и примените исправление",
                        fixedCode = targetCode
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isReviewing = false,
                    reviewMarkdown = "Ошибка ревью: ${e.localizedMessage}"
                )
            }
        }
    }

    fun deleteReviewRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteReview(id)
        }
    }
}
