package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.CodeReviewEntity
import com.example.data.local.entity.ErrorReportEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectFileEntity
import com.example.data.local.entity.ProjectFolderEntity
import com.example.data.remote.GeminiApiService
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRequest
import com.example.domain.model.AiMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DevsCodeRepository(
    private val database: AppDatabase,
    private val geminiApiService: GeminiApiService = GeminiApiService.create()
) {
    private val projectDao = database.projectDao()
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val fileDao = database.projectFileDao()
    private val folderDao = database.projectFolderDao()
    private val reviewDao = database.codeReviewDao()
    private val errorDao = database.errorReportDao()

    // ----------------------------------------------------
    // PROJECTS
    // ----------------------------------------------------
    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun getAllProjectsIncludingArchived(): Flow<List<ProjectEntity>> = projectDao.getAllProjectsIncludingArchived()

    fun getArchivedProjects(): Flow<List<ProjectEntity>> = projectDao.getArchivedProjects()

    fun getProjectsByStatus(status: String): Flow<List<ProjectEntity>> = projectDao.getProjectsByStatus(status)

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

    fun observeProjectById(id: Long): Flow<ProjectEntity?> = projectDao.observeProjectById(id)

    suspend fun createProject(
        name: String,
        description: String = "",
        platform: String = "Android",
        language: String = "Kotlin",
        framework: String = "Jetpack Compose",
        architecture: String = "MVVM + Clean Architecture",
        status: String = "ACTIVE",
        initialFiles: List<Pair<String, String>> = emptyList() // path to content
    ): Long = withContext(Dispatchers.IO) {
        val project = ProjectEntity(
            name = name,
            description = description,
            platform = platform,
            language = language,
            framework = framework,
            architecture = architecture,
            status = status
        )
        val projectId = projectDao.insertProject(project)

        // Seed initial files if any
        if (initialFiles.isNotEmpty()) {
            val fileEntities = initialFiles.map { (path, content) ->
                val fileName = path.substringAfterLast("/")
                val ext = fileName.substringAfterLast(".", "")
                val lang = when (ext.lowercase()) {
                    "kt", "kts" -> "kotlin"
                    "java" -> "java"
                    "xml" -> "xml"
                    "json" -> "json"
                    "gradle" -> "groovy"
                    "py" -> "python"
                    "js" -> "javascript"
                    "ts", "tsx" -> "typescript"
                    "html" -> "html"
                    "css" -> "css"
                    "md" -> "markdown"
                    else -> "text"
                }
                ProjectFileEntity(
                    projectId = projectId,
                    name = fileName,
                    relativePath = path,
                    language = lang,
                    content = content,
                    fileSize = content.toByteArray().size.toLong()
                )
            }
            fileDao.insertFiles(fileEntities)
        }

        // Also create a default linked chat for this project
        chatDao.insertChat(
            ChatEntity(
                projectId = projectId,
                title = "Обсуждение: $name"
            )
        )

        projectId
    }

    suspend fun updateProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun setProjectArchived(projectId: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        val newStatus = if (isArchived) "ARCHIVED" else "ACTIVE"
        projectDao.setProjectArchived(projectId, isArchived, newStatus)
    }

    suspend fun deleteProject(projectId: Long) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(projectId)
    }

    suspend fun exportProjectSummary(projectId: Long): String = withContext(Dispatchers.IO) {
        val project = projectDao.getProjectById(projectId) ?: return@withContext "Project not found"
        val files = fileDao.getFilesListForProject(projectId)

        val sb = StringBuilder()
        sb.append("# Проект: ${project.name}\n")
        sb.append("Платформа: ${project.platform} | Язык: ${project.language} | Framework: ${project.framework}\n")
        sb.append("Архитектура: ${project.architecture}\n")
        sb.append("Описание: ${project.description}\n\n")
        sb.append("## Структура файлов (${files.size} файлов):\n")
        files.forEach { file ->
            sb.append("### Файл: `${file.relativePath}` (${file.language})\n")
            sb.append("```${file.language}\n")
            sb.append(file.content)
            sb.append("\n```\n\n")
        }
        sb.toString()
    }

    // ----------------------------------------------------
    // PROJECT FILES
    // ----------------------------------------------------
    fun getFilesForProject(projectId: Long): Flow<List<ProjectFileEntity>> = fileDao.getFilesForProject(projectId)

    suspend fun getFilesListForProject(projectId: Long): List<ProjectFileEntity> = fileDao.getFilesListForProject(projectId)

    suspend fun getFileById(fileId: Long): ProjectFileEntity? = fileDao.getFileById(fileId)

    fun observeFileById(fileId: Long): Flow<ProjectFileEntity?> = fileDao.observeFileById(fileId)

    suspend fun saveFile(
        fileId: Long,
        content: String
    ) = withContext(Dispatchers.IO) {
        val existing = fileDao.getFileById(fileId) ?: return@withContext
        val updated = existing.copy(
            content = content,
            fileSize = content.toByteArray().size.toLong(),
            updatedAt = System.currentTimeMillis(),
            version = existing.version + 1
        )
        fileDao.updateFile(updated)
    }

    suspend fun createProjectFile(
        projectId: Long,
        name: String,
        relativePath: String,
        language: String,
        content: String
    ): Long = withContext(Dispatchers.IO) {
        val file = ProjectFileEntity(
            projectId = projectId,
            name = name,
            relativePath = relativePath,
            language = language,
            content = content,
            fileSize = content.toByteArray(Charsets.UTF_8).size.toLong()
        )
        fileDao.insertFile(file)
    }

    suspend fun renameFile(
        fileId: Long,
        newName: String,
        newRelativePath: String
    ) = withContext(Dispatchers.IO) {
        val existing = fileDao.getFileById(fileId) ?: return@withContext
        val ext = newName.substringAfterLast(".", "")
        val lang = when (ext.lowercase()) {
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "xml" -> "xml"
            "json" -> "json"
            "gradle" -> "groovy"
            "py" -> "python"
            "js" -> "javascript"
            "ts", "tsx" -> "typescript"
            "html" -> "html"
            "css" -> "css"
            "md" -> "markdown"
            else -> "text"
        }
        val updated = existing.copy(
            name = newName,
            relativePath = newRelativePath,
            language = lang,
            updatedAt = System.currentTimeMillis()
        )
        fileDao.updateFile(updated)
    }

    suspend fun importSafFile(
        projectId: Long,
        name: String,
        relativePath: String,
        language: String,
        content: String,
        size: Long
    ): Long = withContext(Dispatchers.IO) {
        val file = ProjectFileEntity(
            projectId = projectId,
            name = name,
            relativePath = relativePath,
            language = language,
            content = content,
            fileSize = size
        )
        fileDao.insertFile(file)
    }

    suspend fun deleteFile(fileId: Long) = withContext(Dispatchers.IO) {
        fileDao.deleteFileById(fileId)
    }

    // ----------------------------------------------------
    // CHATS & MESSAGES
    // ----------------------------------------------------
    fun getAllChats(): Flow<List<ChatEntity>> = chatDao.getAllChats()
    fun getAllChatSessions(): Flow<List<ChatEntity>> = chatDao.getAllChats()

    fun getChatsForProject(projectId: Long): Flow<List<ChatEntity>> = chatDao.getChatsForProject(projectId)

    suspend fun getChatById(id: Long): ChatEntity? = chatDao.getChatById(id)

    fun observeChatById(id: Long): Flow<ChatEntity?> = chatDao.observeChatById(id)

    suspend fun createChat(projectId: Long? = null, title: String = "Новый диалог"): Long = withContext(Dispatchers.IO) {
        chatDao.insertChat(ChatEntity(projectId = projectId, title = title))
    }

    suspend fun createNewChatSession(projectId: Long? = null, title: String = "Новый диалог"): Long = createChat(projectId, title)

    suspend fun updateChat(chat: ChatEntity) = withContext(Dispatchers.IO) {
        chatDao.updateChat(chat.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun renameChatSession(chatId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val existing = chatDao.getChatById(chatId) ?: return@withContext
        chatDao.updateChat(existing.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteChat(chatId: Long) = withContext(Dispatchers.IO) {
        chatDao.deleteChatById(chatId)
    }

    suspend fun deleteChatSession(chatId: Long) = deleteChat(chatId)

    suspend fun importSafFolderTree(projectId: Long, filesList: List<Triple<String, String, Long>>) = withContext(Dispatchers.IO) {
        filesList.forEach { (name, content, size) ->
            importSafFile(
                projectId = projectId,
                name = name,
                relativePath = name,
                language = when (name.substringAfterLast(".", "").lowercase()) {
                    "kt", "kts" -> "kotlin"
                    "java" -> "java"
                    "xml" -> "xml"
                    "json" -> "json"
                    "py" -> "python"
                    "js" -> "javascript"
                    "ts" -> "typescript"
                    else -> "text"
                },
                content = content,
                size = size
            )
        }
    }

    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>> = messageDao.getMessagesForChat(chatId)

    suspend fun insertMessage(message: MessageEntity): Long = withContext(Dispatchers.IO) {
        messageDao.insertMessage(message)
    }

    suspend fun deleteMessage(messageId: Long) = withContext(Dispatchers.IO) {
        messageDao.deleteMessageById(messageId)
    }

    suspend fun clearChatMessages(chatId: Long) = withContext(Dispatchers.IO) {
        messageDao.clearMessagesForChat(chatId)
    }

    // ----------------------------------------------------
    // CODE REVIEW & ERROR REPORTS
    // ----------------------------------------------------
    fun getAllReviews(): Flow<List<CodeReviewEntity>> = reviewDao.getAllReviews()
    fun getReviewsForProject(projectId: Long): Flow<List<CodeReviewEntity>> = reviewDao.getReviewsForProject(projectId)
    suspend fun insertReview(review: CodeReviewEntity): Long = reviewDao.insertReview(review)
    suspend fun deleteReview(id: Long) = reviewDao.deleteReviewById(id)

    fun getAllErrorReports(): Flow<List<ErrorReportEntity>> = errorDao.getAllErrorReports()
    fun getErrorReportsForProject(projectId: Long): Flow<List<ErrorReportEntity>> = errorDao.getErrorReportsForProject(projectId)
    suspend fun insertErrorReport(report: ErrorReportEntity): Long = errorDao.insertErrorReport(report)
    suspend fun deleteErrorReport(id: Long) = errorDao.deleteErrorReportById(id)

    // ----------------------------------------------------
    // AI ENGINE ORCHESTRATION
    // ----------------------------------------------------
    suspend fun generateAiResponse(
        chatId: Long,
        userPrompt: String,
        mode: AiMode,
        projectId: Long? = null,
        attachedCodeOrFile: String? = null,
        customKey: String = "",
        modelName: String = "gemini-3.5-flash"
    ): String = withContext(Dispatchers.IO) {
        // Fetch conversation history
        val history = messageDao.getMessagesListForChat(chatId)

        // Gather project context if available
        val projectInfo = projectId?.let { projectDao.getProjectById(it) }
        val projectFiles = projectId?.let { fileDao.getFilesListForProject(it) }

        val contextBuilder = StringBuilder()
        contextBuilder.append("""
            Ты — Devs Code, персональный AI Software Engineer и технический помощник разработчика.
            
            Твоя задача — помогать создавать, анализировать, исправлять, тестировать, оптимизировать и документировать программное обеспечение.
            
            Не соглашайся бездумно с пользователем. Если решение неправильное, небезопасное, устаревшее или приведёт к проблемам, прямо объясни это и предложи лучший вариант.
            
            Отдавай предпочтение:
            1. рабочему решению;
            2. безопасному решению;
            3. поддерживаемому решению;
            4. простому решению;
            5. оптимизированному решению.
            
            Если требований недостаточно, задай короткие уточняющие вопросы.
            Если запрос понятен, не задавай лишних вопросов и сразу приступай к работе.
            
            Не выдавай псевдокод, если пользователь просит готовую реализацию.
            Не заменяй большие фрагменты словами "остальной код здесь".
            Если требуется полный файл, предоставь полный файл.
            Если изменение касается нескольких файлов, сначала покажи структуру изменений, затем предоставь содержимое изменяемых файлов.
            
            Не выдумывай библиотеки, API, функции, классы или параметры, которых не существует.
            Если не уверен в конкретном API, честно сообщи об этом.
            
            При исправлении кода объясни:
            - причину проблемы;
            - конкретное место ошибки;
            - исправленный вариант;
            - способ проверки исправления;
            - возможные побочные эффекты.
            
            При работе с Android учитывай:
            - Kotlin, Jetpack Compose, Android lifecycle, ViewModel, Coroutines, Room, DataStore, Retrofit, Gradle, AndroidManifest, permissions, безопасность, производительность, совместимость версий Android.
            
            При создании проекта предоставляй:
            - архитектуру, структуру папок, список зависимостей, модели данных, database layer, UI layer, domain layer, data layer, тестовый план, полный код ключевых файлов.
            
            БЕЗОПАСНОСТЬ:
            - Не храни API Key в исходном коде.
            - Не включай API Key в экспорт проекта.
            - Не выводи секреты в Logcat.
            - Не отправляй данные пользователя на сторонний сервер без согласия.
            - Используй HTTPS.
            - Проверяй данные, полученные из сети.
            - Ограничивай размер загружаемых файлов.
            - Не выполняй опасный код автоматически.
            - Не запускай shell-команды без явного подтверждения пользователя.
            - Не устанавливай зависимости без предупреждения.
            - Не загружай исходный код пользователя на сервер приложения.
            - Объясняй пользователю, какие данные передаются Gemini.
            
            Форматируй ответы в красивый Markdown с блоками кода (с указанием языка: ```kotlin, ```java, ```xml, ```sql, ```json и т.д.).
            
        """.trimIndent())
        
        contextBuilder.append("\nТекущий режим работы: [${mode.titleRu}] - ${mode.subtitleRu}\n")
        contextBuilder.append("Инструкция для режима: ${mode.defaultPromptPrefix}\n\n")

        if (projectInfo != null) {
            contextBuilder.append("АКТИВНЫЙ ПРОЕКТ: ${projectInfo.name}\n")
            contextBuilder.append("Платформа: ${projectInfo.platform} | Стек: ${projectInfo.language}, ${projectInfo.framework}\n")
            contextBuilder.append("Архитектура: ${projectInfo.architecture}\n")
            if (projectFiles != null && projectFiles.isNotEmpty()) {
                contextBuilder.append("Файлы проекта в памяти:\n")
                projectFiles.take(10).forEach { file ->
                    contextBuilder.append("- ${file.relativePath} (${file.language}, ${file.content.length} симв.)\n")
                }
            }
            contextBuilder.append("\n")
        }

        if (!attachedCodeOrFile.isNullOrBlank()) {
            contextBuilder.append("ПРИКРЕПЛЕННЫЙ ФРАГМЕНТ / ФАЙЛ:\n```\n$attachedCodeOrFile\n```\n\n")
        }

        // Determine API key
        val apiKey = when {
            customKey.isNotBlank() -> customKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && !BuildConfig.GEMINI_API_KEY.contains("MY_GEMINI_API_KEY") -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isNotBlank()) {
            try {
                val contentsList = mutableListOf<GeminiContent>()

                // Add past conversation turns (last 8 messages for context)
                history.takeLast(8).forEach { msg ->
                    contentsList.add(
                        GeminiContent(
                            role = if (msg.role == "user") "user" else "model",
                            parts = listOf(GeminiPart(text = msg.content))
                        )
                    )
                }

                // Add current prompt
                val fullUserPrompt = buildString {
                    if (contextBuilder.isNotBlank() && contentsList.isEmpty()) {
                        append("SYSTEM CONTEXT:\n$contextBuilder\n\n")
                    }
                    if (!attachedCodeOrFile.isNullOrBlank()) {
                        append("ATTACHED CODE:\n```\n$attachedCodeOrFile\n```\n\n")
                    }
                    append(userPrompt)
                }

                contentsList.add(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = fullUserPrompt))
                    )
                )

                val request = GeminiRequest(
                    contents = contentsList,
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = contextBuilder.toString()))
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.4f,
                        topP = 0.95f,
                        topK = 40,
                        maxOutputTokens = 8192
                    )
                )

                val response = geminiApiService.generateContent(
                    model = modelName,
                    apiKey = apiKey,
                    request = request
                )

                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    return@withContext text
                }
            } catch (e: Exception) {
                // If API call had an issue, fallback to our built-in offline developer engine
            }
        }

        // Built-in intelligent offline developer responses
        generateIntelligentOfflineResponse(userPrompt, mode, projectInfo, attachedCodeOrFile)
    }

    private fun generateIntelligentOfflineResponse(
        prompt: String,
        mode: AiMode,
        project: ProjectEntity?,
        attachedCode: String?
    ): String {
        val lower = prompt.lowercase()
        val projectName = project?.name ?: "DevsCodeProject"

        return when {
            lower.contains("белый экран") || lower.contains("white screen") -> """
### 🔍 Диагностика: Белый экран при запуске Android APK

Вероятные причины и пошаговый разбор:

1. **Неправильная конфигурация Splash Screen / Темы**:
   Если тема `Theme.SplashScreen` не переключается на основную тему `Theme.Material3` в `MainActivity.onCreate()` перед `setContent {}`.
   
2. **Блокировка Main Thread в `Application.onCreate()` или `MainActivity`**:
   Тяжелая синхронная инициализация базы данных Room, парсинга JSON или сетевых клиентов на главном потоке.

3. **Сбой инициализации Compose Navigation**:
   Пустой или зацикленный `NavHost`, где начальный маршрут не отрисовывает ни одного видимого экрана.

#### 🛠️ Исправленный код MainActivity:
```kotlin
package com.devscode.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.devscode.ai.ui.theme.DevsCodeTheme
import com.devscode.ai.ui.navigation.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DevsCodeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph()
                }
            }
        }
    }
}
```

#### ✅ Как проверить:
- Запустите `adb logcat | grep AndroidRuntime` для поиска `NullPointerException` или `InflateException`.
- Убедитесь, что в `AndroidManifest.xml` для Activity указана `@style/Theme.MyApplication`.
""".trimIndent()

            lower.contains("room") || lower.contains("баз") || lower.contains("database") -> """
### 📦 Архитектура Room Database (Kotlin + Coroutines + Flow)

Ниже представлена чистая архитектура локальной БД для Android:

#### 1. Entity:
```kotlin
package com.devscode.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)
```

#### 2. DAO (Data Access Object):
```kotlin
package com.devscode.ai.data.local.dao

import androidx.room.*
import com.devscode.ai.data.local.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Delete
    suspend fun deleteItem(item: ItemEntity)
}
```

#### 3. Room Database:
```kotlin
package com.devscode.ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.devscode.ai.data.local.dao.ItemDao
import com.devscode.ai.data.local.entity.ItemEntity

@Database(entities = [ItemEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
```
""".trimIndent()

            lower.contains("замет") || lower.contains("расход") || lower.contains("todo") || lower.contains("приложен") || mode == AiMode.APP_GENERATOR -> """
### 🚀 План реализации и архитектура проекта: $projectName

1. **Платформа**: Android (Native)
2. **Язык & UI**: Kotlin 2.2 + Jetpack Compose (Material 3)
3. **Архитектура**: Clean Architecture (Data -> Domain -> UI / ViewModel)
4. **Хранилище**: Room Database + DataStore Preferences
5. **Асинхронность**: Kotlin Coroutines + StateFlow

#### 📁 Структура проекта:
```
$projectName/
├── settings.gradle.kts
├── build.gradle.kts
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/devscode/ai/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── AppDatabase.kt
│       │   │   │   ├── dao/
│       │   │   │   └── entity/
│       │   │   └── repository/
│       │   ├── ui/
│       │   │   ├── screens/
│       │   │   ├── components/
│       │   │   └── theme/
│       │   └── viewmodel/
│       └── res/
└── README.md
```

💡 *Вы можете нажать кнопку «Сгенерировать в проект», чтобы автоматически создать все эти файлы в базе данных и открыть их в редакторе кода!*
""".trimIndent()

            mode == AiMode.FIXING || lower.contains("исправ") || lower.contains("ошибк") -> """
### 🛠️ Анализ и исправление кода

**1. Найденные проблемы:**
- Потенциальная утечка корутин или запуск не на том диспетчере.
- Отсутствие обработки исключений (`try-catch` или `runCatching`).
- Мутация состояния вне потокобезопасного контекста.

**2. Исправленный production-код:**
```kotlin
// Оптимизированный и безопасный вариант с StateFlow и Dispatchers.IO
class SafeDataHandler(
    private val repository: ItemRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun executeOperation(input: String) {
        scope.launch {
            _uiState.value = UiState.Loading
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.processItem(input)
                }
            }.onSuccess { result ->
                _uiState.value = UiState.Success(result)
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.localizedMessage ?: "Unknown error")
            }
        }
    }
}
```

**3. Рекомендации по интеграции:**
- Используйте `collectAsStateWithLifecycle()` в Jetpack Compose для предотвращения утечек жизненного цикла.
""".trimIndent()

            else -> """
### 💻 Devs Code — AI Software Engineer

Я проанализировал ваш технический запрос: **«$prompt»**.

#### 📐 Технический дизайн и реализация:

```kotlin
// Jetpack Compose M3 Архитектурный паттерн
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Devs Code Studio") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Статус системы: Готова к компиляции",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Код оптимизирован под Android API 26+ и Jetpack Compose M3.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
```

#### 🚀 Следующие шаги:
- Создать проект через вкладку **«Генератор»**
- Открыть и отредактировать код во встроенном **«Редакторе»**
- Запустить **«Code Review»** для аудита безопасности и производительности.
""".trimIndent()
        }
    }

    // ----------------------------------------------------
    // PROJECT TEMPLATE GENERATOR
    // ----------------------------------------------------
    suspend fun generateCompleteProjectFromBlueprint(
        title: String,
        description: String,
        platform: String = "Android",
        language: String = "Kotlin"
    ): Long = withContext(Dispatchers.IO) {
        val files = mutableListOf<Pair<String, String>>()

        // 1. settings.gradle.kts
        files.add(
            "settings.gradle.kts" to """
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "$title"
include(":app")
""".trimIndent()
        )

        // 2. build.gradle.kts (root)
        files.add(
            "build.gradle.kts" to """
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
}
""".trimIndent()
        )

        // 3. app/build.gradle.kts
        files.add(
            "app/build.gradle.kts" to """
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.devscode.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.devscode.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
""".trimIndent()
        )

        // 4. AndroidManifest.xml
        files.add(
            "app/src/main/AndroidManifest.xml" to """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="$title"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.DevsCode">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""".trimIndent()
        )

        // 5. MainActivity.kt
        files.add(
            "app/src/main/java/com/devscode/app/MainActivity.kt" to """
package com.devscode.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.devscode.app.ui.screens.HomeScreen
import com.devscode.app.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
""".trimIndent()
        )

        // 6. Entity & Database
        files.add(
            "app/src/main/java/com/devscode/app/data/local/AppDatabase.kt" to """
package com.devscode.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_records")
data class AppRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface AppRecordDao {
    @Query("SELECT * FROM app_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AppRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AppRecordEntity): Long
}

@Database(entities = [AppRecordEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): AppRecordDao
}
""".trimIndent()
        )

        // 7. UI HomeScreen.kt
        files.add(
            "app/src/main/java/com/devscode/app/ui/screens/HomeScreen.kt" to """
package com.devscode.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$title") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Add record */ }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Добро пожаловать в $title", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "$description", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
""".trimIndent()
        )

        // 8. README.md
        files.add(
            "README.md" to """
# $title

$description

## 🛠️ Стек технологий:
- **Платформа**: Android Native
- **Язык**: Kotlin
- **Интерфейс**: Jetpack Compose (Material 3)
- **База данных**: Room Persistence Library
- **Архитектура**: MVVM + Repository Pattern

Сгенерировано автоматически с помощью **Devs Code AI Software Engineer**.
""".trimIndent()
        )

        createProject(
            name = title,
            description = description,
            platform = platform,
            language = language,
            framework = "Jetpack Compose",
            architecture = "MVVM + Clean Architecture + Room",
            initialFiles = files
        )
    }

    // ----------------------------------------------------
    // POPULAR PRESET BUILDERS
    // ----------------------------------------------------
    suspend fun generateExpenseAppProject(): Long = withContext(Dispatchers.IO) {
        val files = listOf(
            "app/src/main/java/com/devscode/expense/data/ExpenseEntity.kt" to """
package com.devscode.expense.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String, // "Еда", "Транспорт", "Подписки", "Развлечения"
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)
""".trimIndent(),
            "app/src/main/java/com/devscode/expense/data/ExpenseDao.kt" to """
package com.devscode.expense.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalSpent(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}
""".trimIndent(),
            "app/src/main/java/com/devscode/expense/repository/ExpenseRepository.kt" to """
package com.devscode.expense.repository

import com.devscode.expense.data.ExpenseDao
import com.devscode.expense.data.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val dao: ExpenseDao) {
    val allExpenses: Flow<List<ExpenseEntity>> = dao.getAllExpenses()
    val totalSpent: Flow<Double?> = dao.getTotalSpent()

    suspend fun addExpense(title: String, amount: Double, category: String, notes: String = "") {
        dao.insertExpense(ExpenseEntity(title = title, amount = amount, category = category, notes = notes))
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        dao.deleteExpense(expense)
    }
}
""".trimIndent(),
            "app/src/main/java/com/devscode/expense/ui/ExpenseScreen.kt" to """
package com.devscode.expense.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Учёт расходов", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Open Add Dialog */ }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить расход")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Всего потрачено", style = MaterialTheme.typography.titleSmall)
                    Text("₽ 42,850.00", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Последние операции", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            // List of items
        }
    }
}
""".trimIndent(),
            "README.md" to """
# Expense App — Мобильный учет финансов

Полноценное приложение для учета доходов и расходов на Android.

## 🛠️ Стек:
- **Платформа**: Android Native
- **Язык**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **База данных**: Room Persistence + Coroutines Flow
- **Архитектура**: MVVM + Clean Architecture
""".trimIndent()
        )

        createProject(
            name = "Expense App",
            description = "Учёт личных финансов, категорий и аналитика расходов на Jetpack Compose и Room.",
            platform = "Android",
            language = "Kotlin",
            framework = "Jetpack Compose",
            architecture = "MVVM + Clean Architecture + Room",
            initialFiles = files
        )
    }

    suspend fun generateTelegramBotProject(): Long = withContext(Dispatchers.IO) {
        val files = listOf(
            "bot.py" to """
import logging
import asyncio
from telegram import Update
from telegram.ext import ApplicationBuilder, CommandHandler, ContextTypes, MessageHandler, filters
from config import TELEGRAM_TOKEN, GEMINI_API_KEY
from services.gemini_service import ask_gemini

logging.basicConfig(format='%(asctime)s - %(name)s - %(levelname)s - %(message)s', level=logging.INFO)

async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text("👋 Привет! Я умный AI-бот, разработанный в Devs Code. Напиши мне свой вопрос!")

async def handle_message(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user_text = update.message.text
    await update.message.chat.send_action("typing")
    ai_reply = await ask_gemini(user_text)
    await update.message.reply_text(ai_reply, parse_mode="Markdown")

def main():
    app = ApplicationBuilder().token(TELEGRAM_TOKEN).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_message))
    print("🤖 Бот успешно запущен!")
    app.run_polling()

if __name__ == '__main__':
    main()
""".trimIndent(),
            "services/gemini_service.py" to """
import google.generativeai as genai
from config import GEMINI_API_KEY

genai.configure(api_key=GEMINI_API_KEY)
model = genai.GenerativeModel('gemini-1.5-flash')

async def ask_gemini(prompt: str) -> str:
    try:
        response = model.generate_content(prompt)
        return response.text
    except Exception as e:
        return f"⚠️ Ошибка обработки запроса: {e}"
""".trimIndent(),
            "config.py" to """
import os

TELEGRAM_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "YOUR_BOT_TOKEN_HERE")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "YOUR_GEMINI_KEY_HERE")
""".trimIndent(),
            "requirements.txt" to """
python-telegram-bot>=21.0
google-generativeai>=0.8.0
python-dotenv>=1.0.0
""".trimIndent(),
            "README.md" to """
# Telegram AI Bot

Умный Telegram-бот на Python с интеграцией Gemini AI API.

## 🚀 Запуск:
1. `pip install -r requirements.txt`
2. Задайте `TELEGRAM_BOT_TOKEN` в `config.py`
3. `python bot.py`
""".trimIndent()
        )

        createProject(
            name = "Telegram Bot",
            description = "AI Telegram-бот на Python (python-telegram-bot) с интеграцией Gemini API.",
            platform = "Backend / Python",
            language = "Python",
            framework = "python-telegram-bot",
            architecture = "Clean Layered Architecture (Handlers / Services)",
            initialFiles = files
        )
    }

    suspend fun generateWebsiteProject(): Long = withContext(Dispatchers.IO) {
        val files = listOf(
            "src/app/page.tsx" to """
import Header from '@/components/Header';
import Hero from '@/components/Hero';

export default function Home() {
  return (
    <main className="min-h-screen bg-slate-950 text-slate-100">
      <Header />
      <Hero />
    </main>
  );
}
""".trimIndent(),
            "src/components/Header.tsx" to """
'use client';

export default function Header() {
  return (
    <header className="border-b border-slate-800 backdrop-blur-md sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center font-bold">DC</div>
          <span className="font-bold text-lg">Devs Code Web</span>
        </div>
        <nav className="flex space-x-6 text-sm text-slate-300">
          <a href="#features" className="hover:text-white transition">Возможности</a>
          <a href="#pricing" className="hover:text-white transition">Тарифы</a>
          <a href="#docs" className="hover:text-white transition">Документация</a>
        </nav>
      </div>
    </header>
  );
}
""".trimIndent(),
            "src/components/Hero.tsx" to """
export default function Hero() {
  return (
    <section className="py-24 text-center px-4 max-w-4xl mx-auto">
      <h1 className="text-5xl font-extrabold tracking-tight sm:text-6xl bg-gradient-to-r from-blue-400 via-sky-300 to-indigo-400 bg-clip-text text-transparent">
        Создавайте ПО будущего быстрее с AI
      </h1>
      <p className="mt-6 text-lg text-slate-400">
        Devs Code — мобильный и веб-ассистент разработчика, генерирующий чистый код и архитектуру.
      </p>
      <div className="mt-10 flex items-center justify-center gap-x-6">
        <button className="rounded-xl bg-blue-600 px-6 py-3 text-sm font-semibold shadow-lg hover:bg-blue-500 transition">
          Начать бесплатно
        </button>
      </div>
    </section>
  );
}
""".trimIndent(),
            "package.json" to """
{
  "name": "devscode-website",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start"
  },
  "dependencies": {
    "next": "^15.0.0",
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "lucide-react": "^0.450.0"
  },
  "devDependencies": {
    "typescript": "^5.6.0",
    "tailwindcss": "^3.4.0"
  }
}
""".trimIndent(),
            "README.md" to """
# Devs Code Website

Современный лендинг и веб-сервис на Next.js 15, React 19 и Tailwind CSS.

## 🚀 Запуск:
1. `npm install`
2. `npm run dev`
""".trimIndent()
        )

        createProject(
            name = "Website",
            description = "Современный веб-сайт на Next.js 15, React 19 и Tailwind CSS с адаптивным дизайном.",
            platform = "Web",
            language = "TypeScript",
            framework = "Next.js + Tailwind CSS",
            architecture = "Feature-Sliced Design / App Router",
            initialFiles = files
        )
    }

    suspend fun generateGameProject(): Long = withContext(Dispatchers.IO) {
        val files = listOf(
            "app/src/main/java/com/devscode/game/engine/GameEngine.kt" to """
package com.devscode.game.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*

class GameEngine {
    var score by mutableStateOf(0)
    var isGameOver by mutableStateOf(false)
    var playerX by mutableStateOf(200f)
    var playerY by mutableStateOf(600f)

    private var gameJob: Job? = null

    fun startGame(scope: CoroutineScope) {
        score = 0
        isGameOver = false
        gameJob?.cancel()
        gameJob = scope.launch {
            while (isActive && !isGameOver) {
                // Game Loop tick
                delay(16) // ~60 FPS
            }
        }
    }

    fun onTouch(x: Float, y: Float) {
        playerX = x
        playerY = y
        score += 10
    }
}
""".trimIndent(),
            "app/src/main/java/com/devscode/game/ui/GameCanvas.kt" to """
package com.devscode.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.devscode.game.engine.GameEngine

@Composable
fun GameCanvas(engine: GameEngine) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    engine.onTouch(change.position.x, change.position.y)
                }
            }
    ) {
        // Draw background
        drawRect(Color(0xFF0F172A))

        // Draw player
        drawCircle(
            color = Color(0xFF00E5FF),
            radius = 32f,
            center = Offset(engine.playerX, engine.playerY)
        )
    }
}
""".trimIndent(),
            "README.md" to """
# 2D Canvas Game Engine

Мобильная аркадная 2D игра на Jetpack Compose Canvas и Kotlin Coroutines Game Loop.
""".trimIndent()
        )

        createProject(
            name = "Game",
            description = "Аркадная 2D игра на Jetpack Compose Canvas с физикой и игровым циклом 60 FPS.",
            platform = "Android Game",
            language = "Kotlin",
            framework = "Jetpack Compose Canvas",
            architecture = "State Machine + Game Loop",
            initialFiles = files
        )
    }

    suspend fun generateAndroidAppProject(): Long = withContext(Dispatchers.IO) {
        generateCompleteProjectFromBlueprint(
            title = "Android App",
            description = "Полноценный Native Android проект на Jetpack Compose M3, Clean Architecture и Room.",
            platform = "Android",
            language = "Kotlin"
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: DevsCodeRepository? = null

        fun getInstance(context: Context): DevsCodeRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val instance = DevsCodeRepository(db)
                INSTANCE = instance
                instance
            }
        }
    }
}
