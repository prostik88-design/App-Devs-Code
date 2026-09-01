package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.CodeReviewEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectFileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ProjectExportUtils {

    private val SENSITIVE_PATTERNS = listOf(
        Regex("(?i)(api[_-]?key|secret|password|token|bearer|auth)\\s*[:=]\\s*['\"][a-zA-Z0-9_\\-]{16,}['\"]"),
        Regex("AIzaSy[a-zA-Z0-9_\\-]{33}")
    )

    /**
     * Sanitizes source code to ensure no secret tokens or keys are leaked in exported archives.
     */
    fun sanitizeContent(content: String): String {
        var sanitized = content
        for (pattern in SENSITIVE_PATTERNS) {
            sanitized = pattern.replace(sanitized, "apiKey = \"***REDACTED_BY_DEVS_CODE***\"")
        }
        return sanitized
    }

    /**
     * Builds and writes a clean ZIP archive of the given project to the provided OutputStream.
     */
    suspend fun writeProjectZip(
        project: ProjectEntity,
        files: List<ProjectFileEntity>,
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        ZipOutputStream(outputStream).use { zipOut ->
            val hasReadme = files.any { it.name.equals("README.md", ignoreCase = true) }

            // Write all project files
            for (file in files) {
                if (file.name.equals(".env", ignoreCase = true) || file.name.equals("local.properties", ignoreCase = true)) {
                    continue // Skip sensitive environment files
                }
                val cleanPath = file.relativePath.trimStart('/')
                val cleanContent = sanitizeContent(file.content)
                val entry = ZipEntry(cleanPath)
                zipOut.putNextEntry(entry)
                zipOut.write(cleanContent.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }

            // If project does not have a README.md, generate a rich one
            if (!hasReadme) {
                val readmeContent = generateReadme(project, files)
                val entry = ZipEntry("README.md")
                zipOut.putNextEntry(entry)
                zipOut.write(readmeContent.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }
        }
    }

    /**
     * Builds and writes a clean ZIP archive of an in-memory template / UI design to the provided OutputStream.
     */
    suspend fun writeTemplateZip(
        title: String,
        description: String,
        files: List<Pair<String, String>>,
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        ZipOutputStream(outputStream).use { zipOut ->
            val hasReadme = files.any { it.first.equals("README.md", ignoreCase = true) }

            for ((path, content) in files) {
                val cleanPath = path.trimStart('/')
                val cleanContent = sanitizeContent(content)
                val entry = ZipEntry(cleanPath)
                zipOut.putNextEntry(entry)
                zipOut.write(cleanContent.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }

            if (!hasReadme) {
                val readme = buildString {
                    appendLine("# $title")
                    appendLine()
                    if (description.isNotBlank()) {
                        appendLine(description)
                        appendLine()
                    }
                    appendLine("## 📁 Структура шаблона (${files.size} файлов):")
                    files.forEach { (p, _) ->
                        appendLine("- `$p`")
                    }
                    appendLine()
                    appendLine("---")
                    appendLine("⚡ *Сгенерировано и экспортировано с помощью **Devs Code** — Mobile AI Software Engineer*")
                }
                val entry = ZipEntry("README.md")
                zipOut.putNextEntry(entry)
                zipOut.write(readme.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }
        }
    }

    /**
     * Generates a rich set of starter code and UI designs for standard templates.
     */
    fun getTemplateFiles(templateKey: String): List<Pair<String, String>> {
        return when (templateKey.lowercase()) {
            "android" -> listOf(
                "build.gradle.kts" to """
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.devscode.template"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.devscode.template"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}
""".trimIndent(),
                "app/src/main/AndroidManifest.xml" to """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="DevsCode App"
        android:theme="@style/Theme.DevsCode">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.DevsCode">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""".trimIndent(),
                "app/src/main/java/com/devscode/template/MainActivity.kt" to """
package com.devscode.template

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.devscode.template.ui.screens.HomeScreen
import com.devscode.template.ui.theme.DevsCodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DevsCodeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen()
                }
            }
        }
    }
}
""".trimIndent(),
                "app/src/main/java/com/devscode/template/ui/screens/HomeScreen.kt" to """
package com.devscode.template.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Devs Code Android App", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Add Action */ }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Jetpack Compose + Clean Architecture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Готовый шаблон Android-приложения с Material 3, ViewModel, Room Database и Coroutines Flow.")
                    }
                }
            }
        }
    }
}
""".trimIndent(),
                "app/src/main/java/com/devscode/template/ui/theme/Theme.kt" to """
package com.devscode.template.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFF10B981),
    background = Color(0xFF0B0F19),
    surface = Color(0xFF111827)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    secondary = Color(0xFF059669),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun DevsCodeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colors, content = content)
}
""".trimIndent(),
                "README.md" to """
# Devs Code — Android Native Template

Полноценный шаблон Android-приложения на Kotlin с Jetpack Compose, Material 3 и Clean Architecture.

## 🚀 Стек:
- Kotlin 2.2
- Jetpack Compose M3
- AndroidX Lifecycle & Navigation
- Coroutines & Flow
""".trimIndent()
            )
            "ios" -> listOf(
                "App/DevsCodeApp.swift" to """
import SwiftUI

@main
struct DevsCodeApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
""".trimIndent(),
                "App/Views/ContentView.swift" to """
import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = ContentViewModel()

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    CardView(
                        title: "SwiftUI + MVVM",
                        subtitle: "Шаблон iOS приложения с поддержкой iOS 17+, Swift 6 и современным дизайном.",
                        icon: "swift"
                    )
                }
                .padding()
            }
            .navigationTitle("Devs Code iOS")
            .toolbar {
                Button(action: { viewModel.addItem() }) {
                    Image(systemName: "plus.circle.fill")
                        .font(.title2)
                }
            }
        }
    }
}
""".trimIndent(),
                "App/Views/Components/CardView.swift" to """
import SwiftUI

struct CardView: View {
    let title: String
    let subtitle: String
    let icon: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: icon)
                    .font(.headline)
                    .foregroundColor(.cyan)
                Text(title)
                    .font(.headline)
                    .fontWeight(.bold)
            }
            Text(subtitle)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(uiColor: .secondarySystemBackground))
        .cornerRadius(14)
    }
}
""".trimIndent(),
                "App/ViewModels/ContentViewModel.swift" to """
import Foundation
import Combine

class ContentViewModel: ObservableObject {
    @Published var items: [String] = []

    func addItem() {
        items.append("Элемент \(items.count + 1)")
    }
}
""".trimIndent(),
                "README.md" to """
# Devs Code — iOS SwiftUI Template

Современный шаблон iOS-приложения на Swift 6 и SwiftUI с архитектурой MVVM.
""".trimIndent()
            )
            "ui" -> listOf(
                "ui/theme/Color.kt" to """
package com.devscode.ui.theme

import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF00E5FF)
val EmeraldGreen = Color(0xFF10B981)
val NeonPurple = Color(0xFFA855F7)
val DarkBackground = Color(0xFF0B0F19)
val DarkSurface = Color(0xFF111827)
val DarkSurfaceCard = Color(0xFF1F2937)
val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
""".trimIndent(),
                "ui/components/GlowButton.kt" to """
package com.devscode.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devscode.ui.theme.EmeraldGreen
import com.devscode.ui.theme.NeonCyan

@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(NeonCyan, EmeraldGreen)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}
""".trimIndent(),
                "ui/components/GlassmorphicCard.kt" to """
package com.devscode.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devscode.ui.theme.DarkSurfaceCard

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurfaceCard.copy(alpha = 0.85f),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
""".trimIndent(),
                "ui/screens/UiCatalogScreen.kt" to """
package com.devscode.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devscode.ui.components.GlassmorphicCard
import com.devscode.ui.components.GlowButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiCatalogScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UI Design System & Components", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GlassmorphicCard {
                    Text("Glassmorphism Card", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Полупрозрачная стеклянная карточка с границей и скруглением 16dp.")
                }
            }
            item {
                GlowButton(
                    text = "Интерактивная кнопка с градиентом",
                    onClick = {}
                )
            }
        }
    }
}
""".trimIndent(),
                "README.md" to """
# Devs Code — UI Design System & Component Library

Набор красивых UI-компонентов для Jetpack Compose: Glassmorphism карточки, неоновые градиентные кнопки, цветовая палитра и темы.
""".trimIndent()
            )
            else -> listOf(
                "src/Main.kt" to """
package com.devscode.app

fun main() {
    println("Hello from Devs Code Template!")
}
""".trimIndent(),
                "README.md" to "# $templateKey Template\n\nСгенерировано в Devs Code."
            )
        }
    }

    /**
     * Exports a template directly to a SAF Uri as a ZIP file.
     */
    suspend fun exportTemplateToZipUri(
        context: Context,
        uri: Uri,
        title: String,
        description: String,
        files: List<Pair<String, String>>
    ) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                writeTemplateZip(title, description, files, os)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "ZIP-архив шаблона сохранён на диск!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ошибка сохранения ZIP: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Generates a comprehensive README.md for the project.
     */
    fun generateReadme(project: ProjectEntity, files: List<ProjectFileEntity>): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        return buildString {
            appendLine("# ${project.name}")
            appendLine()
            if (project.description.isNotBlank()) {
                appendLine(project.description)
                appendLine()
            }
            appendLine("## 🚀 Спецификация проекта")
            appendLine("- **Платформа**: ${project.platform}")
            appendLine("- **Язык**: ${project.language}")
            appendLine("- **Фреймворк**: ${project.framework}")
            appendLine("- **Архитектура**: ${project.architecture}")
            appendLine("- **Статус**: ${project.status}")
            appendLine("- **Последнее обновление**: ${dateFormat.format(Date(project.updatedAt))}")
            appendLine()
            appendLine("## 📁 Структура файлов (${files.size} файлов)")
            files.forEach { file ->
                appendLine("- `${file.relativePath}` _(${file.language})_")
            }
            appendLine()
            appendLine("## 🛠️ Сборка и запуск")
            when (project.platform.lowercase()) {
                "android" -> {
                    appendLine("1. Откройте проект в Android Studio Ladybug или новее.")
                    appendLine("2. Выполните Gradle Sync.")
                    appendLine("3. Запустите на эмуляторе или физическом устройстве.")
                }
                "web", "website" -> {
                    appendLine("1. Установите зависимости:")
                    appendLine("   ```bash")
                    appendLine("   npm install # или yarn / pnpm")
                    appendLine("   ```")
                    appendLine("2. Запустите сервер разработки:")
                    appendLine("   ```bash")
                    appendLine("   npm run dev")
                    appendLine("   ```")
                }
                "backend", "python", "telegram bot" -> {
                    appendLine("1. Установите виртуальное окружение:")
                    appendLine("   ```bash")
                    appendLine("   python -m venv venv")
                    appendLine("   source venv/bin/activate # или venv\\Scripts\\activate на Windows")
                    appendLine("   pip install -r requirements.txt")
                    appendLine("   ```")
                    appendLine("2. Запустите приложение:")
                    appendLine("   ```bash")
                    appendLine("   python bot.py")
                    appendLine("   ```")
                }
                else -> {
                    appendLine("Инструкции по сборке доступны в исходных файлах конфигурации.")
                }
            }
            appendLine()
            appendLine("---")
            appendLine("⚡ *Сгенерировано и экспортировано с помощью **Devs Code** — Mobile AI Software Engineer*")
        }
    }

    /**
     * Exports full project codebase into a single Markdown document.
     */
    fun generateProjectMarkdown(project: ProjectEntity, files: List<ProjectFileEntity>): String {
        return buildString {
            appendLine("# 📦 Проект: ${project.name}")
            appendLine("**Платформа**: ${project.platform} | **Язык**: ${project.language} | **Фреймворк**: ${project.framework}")
            appendLine("**Архитектура**: ${project.architecture} | **Статус**: ${project.status}")
            if (project.description.isNotBlank()) {
                appendLine()
                appendLine("> ${project.description}")
            }
            appendLine()
            appendLine("---")
            appendLine("## 📂 Исходный код файлов (${files.size}):")
            appendLine()

            files.forEach { file ->
                appendLine("### 📄 `${file.relativePath}`")
                appendLine("```${file.language.lowercase()}")
                appendLine(sanitizeContent(file.content))
                appendLine("```")
                appendLine()
            }
        }
    }

    /**
     * Generates a temporary zip file and launches a Share intent.
     */
    suspend fun shareProjectAsZip(
        context: Context,
        project: ProjectEntity,
        files: List<ProjectFileEntity>
    ) = withContext(Dispatchers.IO) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanName = project.name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "project" }
            val zipFile = File(exportDir, "${cleanName}.zip")

            FileOutputStream(zipFile).use { fos ->
                writeProjectZip(project, files, fos)
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Devs Code Проект: ${project.name}")
                putExtra(Intent.EXTRA_TEXT, "Экспорт проекта «${project.name}» (${files.size} файлов) из Devs Code")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Поделиться архивом: ${project.name}.zip")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ошибка экспорта ZIP: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Copies code to clipboard and shows toast.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Код скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
    }

    /**
     * Shares text via standard Android Intent.
     */
    fun shareText(context: Context, title: String, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }

    /**
     * Formats chat messages for export.
     */
    fun exportChatMarkdown(chat: ChatEntity, messages: List<MessageEntity>): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return buildString {
            appendLine("# 💬 Диалог: ${chat.title}")
            appendLine("Дата: ${dateFormat.format(Date(chat.updatedAt))}")
            appendLine()
            appendLine("---")
            appendLine()

            messages.forEach { msg ->
                val sender = when (msg.role) {
                    "user" -> "👤 **Пользователь**"
                    "assistant" -> "🤖 **Devs Code AI** (${msg.modelName})"
                    else -> "⚙️ **Система**"
                }
                appendLine("$sender — _${dateFormat.format(Date(msg.createdAt))}_:")
                appendLine()
                appendLine(msg.content)
                appendLine()
                appendLine("---")
                appendLine()
            }
        }
    }

    /**
     * Formats code review results for export.
     */
    fun exportCodeReviewMarkdown(reviews: List<CodeReviewEntity>): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return buildString {
            appendLine("# 🔍 Отчет Code Review")
            appendLine("Дата анализа: ${dateFormat.format(Date())}")
            appendLine("Всего замечаний: ${reviews.size}")
            appendLine()
            appendLine("---")
            appendLine()

            reviews.forEachIndexed { index, review ->
                val badge = when (review.severity.uppercase()) {
                    "CRITICAL" -> "🚨 КРИТИЧЕСКАЯ ОШИБКА"
                    "WARNING" -> "⚠️ ПРЕДУПРЕЖДЕНИЕ"
                    "IMPROVEMENT" -> "💡 УЛУЧШЕНИЕ"
                    else -> "✅ РЕКОМЕНДАЦИЯ"
                }
                appendLine("## $badge: ${review.title}")
                appendLine()
                appendLine("**Описание:**")
                appendLine(review.description)
                appendLine()
                if (review.suggestion.isNotBlank()) {
                    appendLine("**Рекомендация:**")
                    appendLine(review.suggestion)
                    appendLine()
                }
                if (review.fixedCode.isNotBlank()) {
                    appendLine("**Исправленный код:**")
                    appendLine("```kotlin")
                    appendLine(review.fixedCode)
                    appendLine("```")
                    appendLine()
                }
                appendLine("---")
                appendLine()
            }
        }
    }
}
