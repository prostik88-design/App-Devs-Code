package com.example.ui.screens.androiddev

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DevsCodeRepository
import com.example.domain.model.AiMode
import com.example.ui.components.MarkdownViewer
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AndroidDiagnosticTopic(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val query: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidDeveloperScreen(
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { DevsCodeRepository.getInstance(context) }
    val preferences = remember { UserPreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    var customQuestion by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTopicTitle by remember { mutableStateOf<String?>(null) }
    var diagnosticResponse by remember { mutableStateOf<String?>(null) }

    val topics = listOf(
        AndroidDiagnosticTopic(
            title = "Белый экран при запуске APK",
            description = "Splash screen, блокировка Main Thread, сбои темы и навигации",
            icon = Icons.Default.Warning,
            query = "Разбери детально причины белого экрана при запуске Android приложения, покажи как правильно настроить SplashScreen API и переключение тем в Compose."
        ),
        AndroidDiagnosticTopic(
            title = "Оптимизация Compose рекомпозиций",
            description = "Устранение лагов списков, стабильность параметров, derivedStateOf",
            icon = Icons.Default.Speed,
            query = "Как оптимизировать Jetpack Compose UI: предотвращение лишних рекомпозиций в LazyColumn, использование @Immutable, derivedStateOf, rememberUpdatedState и лямбд."
        ),
        AndroidDiagnosticTopic(
            title = "Миграция Room Database без потери данных",
            description = "AutoMigration, ручные миграции SQL и fallbackToDestructiveMigration",
            icon = Icons.Default.Storage,
            query = "Покажи правильную архитектуру миграций в Room Database: от AutoMigration до ручных SQL скриптов Migration(1, 2) без потери данных пользователя."
        ),
        AndroidDiagnosticTopic(
            title = "Storage Access Framework & Scoped Storage",
            description = "Работа с файлами на Android 10+ (API 29-35), URI, InputStream",
            icon = Icons.Default.Security,
            query = "Как правильно реализовать открытие, чтение и сохранение файлов через Android Storage Access Framework (SAF) с ActivityResultContracts.OpenDocument() в Compose."
        ),
        AndroidDiagnosticTopic(
            title = "ProGuard, R8 и релизная сборка APK/AAB",
            description = "Правила keep, сжатие ресурсов, защита от обфускации Room/Serialization",
            icon = Icons.Default.Android,
            query = "Напиши оптимальный proguard-rules.pro для Android проекта с Jetpack Compose, Room Database, Moshi/Serialization и Coroutines. Чек-лист готовности к релизу."
        )
    )

    fun runQuery(queryText: String, topicName: String) {
        selectedTopicTitle = topicName
        isLoading = true
        diagnosticResponse = null

        scope.launch {
            try {
                val apiKey = preferences.customApiKey.first()
                val model = preferences.selectedModel.first()
                val result = repository.generateAiResponse(
                    chatId = 1L,
                    userPrompt = queryText,
                    mode = AiMode.ANDROID_DEVELOPER,
                    customKey = apiKey,
                    modelName = model
                )
                diagnosticResponse = result
            } catch (e: Exception) {
                diagnosticResponse = "Ошибка: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Android Developer Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("android_dev_drawer_btn")) {
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
            // Header Info Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🤖 Специализированный Android Архитектор",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Экспертная база знаний по Kotlin 2.2, Jetpack Compose, Room, Coroutines, Gradle DSL, WorkManager и оптимизации APK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }
            }

            // Custom Question Input
            OutlinedTextField(
                value = customQuestion,
                onValueChange = { customQuestion = it },
                label = { Text("Задайте вопрос по Android разработке") },
                placeholder = { Text("Например: «Как реализовать виджет рабочего стола на Glance?»") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .testTag("android_question_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Button(
                onClick = {
                    if (customQuestion.isNotBlank()) {
                        runQuery(customQuestion, "Пользовательский вопрос")
                    }
                },
                enabled = customQuestion.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("ask_android_dev_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Получить экспертное решение", fontWeight = FontWeight.Bold)
            }

            // Quick Diagnostic Guides
            Text(
                text = "Диагностические темы и рецепты:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                topics.forEach { topic ->
                    Surface(
                        onClick = { runQuery(topic.query, topic.title) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                topic.icon,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topic.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = topic.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondaryDark
                            )
                        }
                    }
                }
            }

            // Diagnostic Results View
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = NeonCyan)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Android Архитектор готовит решение...", color = NeonCyan)
                }
            } else if (!diagnosticResponse.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💡 Решение: ${selectedTopicTitle ?: ""}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MarkdownViewer(content = diagnosticResponse!!)
                    }
                }
            }
        }
    }
}
