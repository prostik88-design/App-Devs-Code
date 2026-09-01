package com.example.ui.screens.debugger

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ErrorReportEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DevsCodeRepository
import com.example.domain.model.AiMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DebuggerUiState(
    val errorInput: String = "",
    val isAnalyzing: Boolean = false,
    val analysisResult: String? = null,
    val reportsHistory: List<ErrorReportEntity> = emptyList()
)

class DebuggerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DevsCodeRepository.getInstance(application)
    private val preferencesRepository = UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(DebuggerUiState())
    val uiState: StateFlow<DebuggerUiState> = _uiState.asStateFlow()

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
            repository.getAllErrorReports().collect { reports ->
                _uiState.value = _uiState.value.copy(reportsHistory = reports)
            }
        }
    }

    fun setErrorInput(input: String) {
        _uiState.value = _uiState.value.copy(errorInput = input)
    }

    fun analyzeError(customError: String? = null) {
        val targetError = customError ?: _uiState.value.errorInput
        if (targetError.isBlank()) return

        _uiState.value = _uiState.value.copy(isAnalyzing = true, analysisResult = null)

        viewModelScope.launch {
            try {
                val prompt = buildString {
                    append("Ты — AI Debugger. Проанализируй следующую ошибку, Stack Trace, Logcat или сбой сборки Gradle.\n")
                    append("Оформи ответ строго по разделам:\n\n")
                    append("1. 🔍 ВЕРОЯТНАЯ ПРИЧИНА ОШИБКИ (Суть сбоя простыми словами)\n")
                    append("2. 📍 ТОЧНЫЙ ИСТОЧНИК (Где возникла ошибка: файл, класс, метод, строка)\n")
                    append("3. 🛠️ ПОШАГОВОЕ РЕШЕНИЕ И ИСПРАВЛЕННЫЙ КОД (Покажи полный рабочий код в блоке ```...)\n")
                    append("4. ✅ КАК ПРОВЕРИТЬ ИСПРАВЛЕНИЕ (Инструкция по верификации)\n")
                    append("5. ⚠️ ПОБОЧНЫЕ ЭФФЕКТЫ (На что обратить внимание, чтобы не сломать другую логику)\n\n")
                    append("Текст ошибки/лога:\n```\n$targetError\n```")
                }

                val aiResult = repository.generateAiResponse(
                    chatId = 1L,
                    userPrompt = prompt,
                    mode = AiMode.DEBUGGER,
                    customKey = customApiKey,
                    modelName = selectedModel
                )

                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    analysisResult = aiResult
                )

                // Save report to Room
                repository.insertErrorReport(
                    ErrorReportEntity(
                        errorType = "Crash / Build Error",
                        errorText = targetError.take(200),
                        analysis = aiResult,
                        solution = "Исправление приведено в отчете"
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    analysisResult = "Ошибка анализатора: ${e.localizedMessage}"
                )
            }
        }
    }
}
