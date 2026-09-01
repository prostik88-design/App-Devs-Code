package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.AiMode
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiSettingsScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val preferences = remember { UserPreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    val selectedModel by preferences.selectedModel.collectAsState(initial = "gemini-3.5-flash")
    val customApiKey by preferences.customApiKey.collectAsState(initial = "")
    val savedTemperature by preferences.geminiTemperature.collectAsState(initial = 0.7f)
    val savedTopP by preferences.geminiTopP.collectAsState(initial = 0.95f)
    val savedSystemPrompt by preferences.customSystemPrompt.collectAsState(initial = "")

    var apiKeyInput by remember(customApiKey) { mutableStateOf(customApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var temperature by remember(savedTemperature) { mutableFloatStateOf(savedTemperature) }
    var topP by remember(savedTopP) { mutableFloatStateOf(savedTopP) }
    var systemPromptInput by remember(savedSystemPrompt) { mutableStateOf(savedSystemPrompt) }

    var isTestingKey by remember { mutableStateOf(false) }
    var testKeyResult by remember { mutableStateOf<String?>(null) }

    val availableModels = listOf(
        "gemini-3.7-flash" to "Gemini 3.7 Flash",
        "gemini-3.5-flash-lite" to "Gemini 3.5 Flash Lite",
        "gemini-3.1-pro-preview" to "Gemini 3.1 Pro Preview",
        "gemini-3.6-flash" to "Gemini 3.6 Flash",
        "gemini-3.5-flash" to "Gemini 3.5 Flash",
        "gemini-3.1-flash-lite" to "Gemini 3.1 Flash Lite"
    )

    val promptPresets = listOf(
        "Senior Android Staff" to "Ты — ведущий Senior Android Engineer. Пиши чистый, типобезопасный Kotlin код с Material 3 и корутинами без лишних объяснений.",
        "Архитектор Clean Code" to "Ты — Principal Software Architect. Всегда предлагай разделение по слоям (Domain, Data, Presentation) и паттерн MVI.",
        "Лаконичный кодер" to "Отвечай только готовыми блоками кода. Без вступительных слов и заключений."
    )

    val isBuiltInKeyAvailable = remember {
        BuildConfig.GEMINI_API_KEY.isNotBlank() && !BuildConfig.GEMINI_API_KEY.contains("MY_GEMINI_API_KEY")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gemini AI Настройки", fontWeight = FontWeight.Bold)
                        Text("Модели, API Ключи и тонкая настройка", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Key Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini API Ключ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isBuiltInKeyAvailable) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Используется встроенный ключ окружения", color = EmeraldGreen, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Пользовательский API Ключ") },
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Видимость ключа"
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("custom_api_key_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isTestingKey = true
                                    testKeyResult = null
                                    try {
                                        val testKey = apiKeyInput.trim()
                                        val repository = com.example.data.repository.DevsCodeRepository.getInstance(context.applicationContext as android.app.Application)
                                        val result = repository.generateAiResponse(
                                            chatId = -1,
                                            userPrompt = "Ответь одним словом: 'OK'",
                                            mode = AiMode.ANDROID_DEVELOPER,
                                            projectId = null,
                                            attachedCodeOrFile = null,
                                            customKey = testKey,
                                            modelName = selectedModel
                                        )
                                        testKeyResult = if (result.isNotBlank()) "✓ Ключ валиден и готов к работе!" else "Ошибка соединения"
                                    } catch (e: Exception) {
                                        testKeyResult = "✗ Ошибка проверки: ${e.localizedMessage}"
                                    } finally {
                                        isTestingKey = false
                                    }
                                }
                            },
                            enabled = !isTestingKey,
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isTestingKey) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Проверить ключ")
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    preferences.setCustomApiKey(apiKeyInput.trim())
                                    Toast.makeText(context, "API Ключ сохранен", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Сохранить")
                        }
                    }

                    if (testKeyResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = testKeyResult!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testKeyResult!!.startsWith("✓")) EmeraldGreen else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = "Безопасность", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Конфиденциальность и безопасность", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("Devs Code отправляет в Gemini API только текст ваших запросов и явно прикрепленные файлы/код. Ваши API ключи и локальные базы данных не отправляются на сторонние серверы Devs Code. Все запросы уходят напрямую в Google по защищенному протоколу HTTPS.", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark, fontSize = 11.sp, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            }

            // Model Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = NeonPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Выбор модели Gemini", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    availableModels.forEach { (modelId, desc) ->
                        val isSelected = selectedModel == modelId
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                scope.launch {
                                    preferences.setSelectedModel(modelId)
                                }
                            },
                            label = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(modelId, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = TextSecondaryDark)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Parameters Fine Tuning
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Параметры генерации", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Температура: ${String.format("%.2f", temperature)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Меньше — точный и детерминированный код, больше — креативные решения", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark, fontSize = 11.sp)
                    Slider(
                        value = temperature,
                        onValueChange = {
                            temperature = it
                            scope.launch { preferences.setGeminiTemperature(it) }
                        },
                        valueRange = 0.0f..2.0f,
                        steps = 19
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Top-P: ${String.format("%.2f", topP)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = topP,
                        onValueChange = {
                            topP = it
                            scope.launch { preferences.setGeminiTopP(it) }
                        },
                        valueRange = 0.0f..1.0f,
                        steps = 19
                    )
                }
            }

            // System Prompt
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Пользовательский System Prompt", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Пресеты стилей поведения AI:", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(promptPresets) { (name, prompt) ->
                            FilterChip(
                                selected = systemPromptInput == prompt,
                                onClick = {
                                    systemPromptInput = prompt
                                    scope.launch { preferences.setCustomSystemPrompt(prompt) }
                                },
                                label = { Text(name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = systemPromptInput,
                        onValueChange = {
                            systemPromptInput = it
                            scope.launch { preferences.setCustomSystemPrompt(it) }
                        },
                        placeholder = { Text("Инструкция для поведения Gemini во всех режимах...") },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
