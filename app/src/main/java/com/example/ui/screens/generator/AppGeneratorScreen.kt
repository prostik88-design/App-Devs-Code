package com.example.ui.screens.generator

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MarkdownViewer
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppGeneratorScreen(
    onOpenDrawer: () -> Unit = {},
    onOpenProject: (Long) -> Unit = {},
    viewModel: AppGeneratorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var appNameInput by remember { mutableStateOf("MyAwesomeApp") }

    val presets = listOf(
        "Android" to "Создай полноценное Android-приложение на Kotlin с использованием Jetpack Compose, ViewModel, Clean Architecture и Room database.",
        "iOS" to "Создай структуру iOS-приложения на Swift с использованием SwiftUI, архитектуры MVVM и CoreData для локального хранения.",
        "UI" to "Разработай современный, красивый и отзывчивый пользовательский интерфейс (UI) для мобильного приложения с плавной анимацией и поддержкой тем.",
        "Учёт расходов" to "Создай приложение для учёта расходов. Нужны категории, статистика, добавление расходов, локальное хранение в Room и Material 3 тёмная тема.",
        "Заметки и задачи" to "Создай приложение заметок и задач с тегами, поиском, отметкой выполнения и локальной базой данных Room.",
        "Фитнес-трекер" to "Создай фитнес-трекер тренировок с расписанием, таймером упражнений, графиками прогресса и Clean Architecture.",
        "Чат-клиент" to "Создай нативный клиент чата с историей диалогов, списком контактов, поиском и локальным кешем сообщений.",
        "Интернет-магазин" to "Создай мобильный каталог товаров с корзиной, категориями, фильтрами и списком избранного."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Генератор приложений",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("generator_drawer_btn")) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
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
            // Header Description
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🚀 AI Проектировщик и Архитектор ПО",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Опишите ваше приложение на естественном языке. AI проанализирует платформу, стек технологий, спроектирует архитектуру, Room БД, UI-компоненты и создаст готовые файлы проекта.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryDark
                    )
                }
            }

            // Presets
            Text(
                text = "Быстрые шаблоны:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (title, prompt) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            viewModel.setPrompt(prompt)
                            appNameInput = title.replace(" ", "") + "App"
                            viewModel.analyzeAndGenerate(prompt)
                        },
                        label = { Text(title) }
                    )
                }
            }

            // User Prompt Input
            OutlinedTextField(
                value = uiState.prompt,
                onValueChange = { viewModel.setPrompt(it) },
                label = { Text("Описание функционала приложения") },
                placeholder = { Text("Например: «Создай приложение для учёта расходов. Нужны категории, статистика...»") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("generator_prompt_input"),
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Action Button
            Button(
                onClick = { viewModel.analyzeAndGenerate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("generate_blueprint_btn"),
                enabled = uiState.prompt.isNotBlank() && !uiState.isAnalyzing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI анализирует и проектирует структуру...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Спроектировать архитектуру и файлы", fontWeight = FontWeight.Bold)
                }
            }

            // Project Creation Banner if already created
            if (uiState.isProjectCreated && uiState.generatedProjectId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Проект «${uiState.generatedProjectName}» успешно создан!",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Все файлы структуры проекта (settings.gradle, AndroidManifest, Room Database, ViewModel, UI) сохранены в локальную базу данных Room.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onOpenProject(uiState.generatedProjectId!!) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Открыть файлы в Диспетчере и Редакторе")
                        }
                    }
                }
            }

            // Analysis Result & Build Button
            if (!uiState.analysisResult.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📐 Результат анализа и архитектурный чертёж:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MarkdownViewer(content = uiState.analysisResult!!)

                        Spacer(modifier = Modifier.height(16.dp))

                        // App Name input before saving
                        OutlinedTextField(
                            value = appNameInput,
                            onValueChange = { appNameInput = it },
                            label = { Text("Имя создаваемого проекта") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.buildAndSaveProject(appNameInput, uiState.prompt)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_project_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Сгенерировать проект и файлы в Room", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
