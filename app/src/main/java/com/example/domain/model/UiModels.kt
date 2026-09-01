package com.example.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector

enum class AiMode(
    val titleRu: String,
    val subtitleRu: String,
    val icon: ImageVector,
    val defaultPromptPrefix: String
) {
    CREATION(
        titleRu = "Создание",
        subtitleRu = "Проекты, экраны, базы данных, API и компоненты",
        icon = Icons.Default.AddCircle,
        defaultPromptPrefix = "Ты — ведущий AI Software Engineer. Спроектируй и создай чистый, production-ready код со структурой файлов и подробными пояснениями архитектуры:"
    ),
    FIXING(
        titleRu = "Исправление",
        subtitleRu = "Поиск багов, крашей, Gradle и синтаксических ошибок",
        icon = Icons.Default.Build,
        defaultPromptPrefix = "Ты — эксперт по отладке и исправлению ошибок. Найди точную причину проблемы, покажи исправленный код и поясни, почему возник баг:"
    ),
    ANALYSIS(
        titleRu = "Анализ",
        subtitleRu = "Code Review, архитектурный аудит, безопасность",
        icon = Icons.Default.Analytics,
        defaultPromptPrefix = "Ты — Senior Software Architect. Проведи глубокий технический анализ кода, выяви уязвимости, проблемы архитектуры и масштабируемости:"
    ),
    OPTIMIZATION(
        titleRu = "Оптимизация",
        subtitleRu = "Ускорение кода, память, сеть, Compose рекомпозиции",
        icon = Icons.Default.Speed,
        defaultPromptPrefix = "Ты — инженер по производительности. Проанализируй узкие места, утечки памяти, лишние операции и оптимизируй решение:"
    ),
    ANDROID_DEVELOPER(
        titleRu = "Android Developer",
        subtitleRu = "Compose, Room, Coroutines, Gradle DSL, APK/AAB",
        icon = Icons.Default.Android,
        defaultPromptPrefix = "Ты — Principal Android Architect (Kotlin, Jetpack Compose, M3, Clean Architecture, Room, Coroutines). Дай исчерпывающее нативное Android-решение:"
    ),
    DEBUGGER(
        titleRu = "AI Debugger",
        subtitleRu = "Разбор Stack trace, Logcat, Room SQL, Crash reports",
        icon = Icons.Default.BugReport,
        defaultPromptPrefix = "Ты — AI Debugger. Разбери переданный Stack Trace/Logcat/Error. Укажи: 1) Вероятную причину; 2) Источник (файл и строку); 3) Конкретный исправленный код; 4) Проверку и побочные эффекты:"
    ),
    APP_GENERATOR(
        titleRu = "Генератор приложений",
        subtitleRu = "Комплексное проектирование проекта от идеи до файлов",
        icon = Icons.Default.AutoAwesome,
        defaultPromptPrefix = "Ты — архитектор программных систем. Пользователь описывает приложение. Проанализируй: название, платформу, стек, архитектуру, экраны, модель данных, Room БД, API, зависимости и сгенерируй файлы проекта:"
    )
}

enum class SupportedLanguage(
    val id: String,
    val displayName: String,
    val extension: String,
    val mimeType: String
) {
    KOTLIN("kotlin", "Kotlin", ".kt", "text/x-kotlin"),
    JAVA("java", "Java", ".java", "text/x-java-source"),
    PYTHON("python", "Python", ".py", "text/x-python"),
    JAVASCRIPT("javascript", "JavaScript", ".js", "application/javascript"),
    TYPESCRIPT("typescript", "TypeScript", ".ts", "application/typescript"),
    HTML("html", "HTML", ".html", "text/html"),
    CSS("css", "CSS", ".css", "text/css"),
    JSON("json", "JSON", ".json", "application/json"),
    SQL("sql", "SQL", ".sql", "text/x-sql"),
    XML("xml", "XML", ".xml", "application/xml"),
    YAML("yaml", "YAML", ".yaml", "text/yaml"),
    MARKDOWN("markdown", "Markdown", ".md", "text/markdown"),
    BASH("bash", "Bash / Shell", ".sh", "application/x-sh"),
    CPP("cpp", "C++", ".cpp", "text/x-c++src"),
    CSHARP("csharp", "C#", ".cs", "text/x-csharp"),
    RUST("rust", "Rust", ".rs", "text/rust"),
    GO("go", "Go", ".go", "text/x-go"),
    DART("dart", "Dart", ".dart", "text/x-dart"),
    SWIFT("swift", "Swift", ".swift", "text/x-swift");

    companion object {
        fun fromExtension(ext: String): SupportedLanguage {
            val cleanExt = if (ext.startsWith(".")) ext else ".$ext"
            return values().firstOrNull { it.extension.equals(cleanExt, ignoreCase = true) } ?: KOTLIN
        }

        fun fromLanguageName(lang: String): SupportedLanguage {
            return values().firstOrNull { it.id.equals(lang, ignoreCase = true) || it.displayName.equals(lang, ignoreCase = true) } ?: KOTLIN
        }
    }
}

data class QuickPrompt(
    val title: String,
    val prompt: String,
    val mode: AiMode,
    val icon: ImageVector = Icons.Default.Code
)

val QuickPromptsList = listOf(
    QuickPrompt("Создать приложение", "Создай архитектуру и проект для приложения ", AiMode.APP_GENERATOR, Icons.Default.AutoAwesome),
    QuickPrompt("Исправить код", "Найди и исправь ошибку в следующем коде:\n\n", AiMode.FIXING, Icons.Default.Build),
    QuickPrompt("Code Review", "Проведи подробный Code Review этого фрагмента кода с поиском уязвимостей, багов и советами по улучшению:\n\n", AiMode.ANALYSIS, Icons.Default.Analytics),
    QuickPrompt("Android Developer", "Помоги решить задачу для Android (Jetpack Compose, Room, Kotlin):\n\n", AiMode.ANDROID_DEVELOPER, Icons.Default.Android),
    QuickPrompt("Создать проект", "Создай структуру многомодульного проекта со всеми конфигурационными файлами Gradle и README:\n\n", AiMode.CREATION, Icons.Default.AddCircle),
    QuickPrompt("Проанализировать ошибку", "Разбери следующий стек-трейс/ошибку сборки и покажи точное решение:\n\n", AiMode.DEBUGGER, Icons.Default.BugReport)
)
