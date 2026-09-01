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
