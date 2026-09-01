package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    // 1. Home / AI Chat
    object Chat : Screen("chat?chatId={chatId}", "AI Chat", Icons.AutoMirrored.Filled.Chat) {
        fun createRoute(chatId: Long? = null): String {
            return if (chatId != null && chatId > 0) "chat?chatId=$chatId" else "chat"
        }
    }

    // 2. Chats (History & List)
    object Chats : Screen("chats", "Чаты", Icons.AutoMirrored.Filled.List)

    // 3. Chat Details (Deep session analysis & settings)
    object ChatDetail : Screen("chat_detail/{chatId}", "Детали чата", Icons.AutoMirrored.Filled.Chat) {
        fun createRoute(chatId: Long): String = "chat_detail/$chatId"
    }

    // 4. My Projects
    object Projects : Screen("projects", "Проекты", Icons.Default.Folder)

    // 5. Project Details (Overview, Architecture, Stack)
    object ProjectDetail : Screen("project_detail/{projectId}", "Детали проекта", Icons.Default.FolderSpecial) {
        fun createRoute(projectId: Long): String = "project_detail/$projectId"
    }

    // 6. Project Files (Explorer & Tree)
    object ProjectFiles : Screen("project_files/{projectId}", "Файлы проекта", Icons.Default.Layers) {
        fun createRoute(projectId: Long): String = "project_files/$projectId"
    }

    // 7. File Viewer (Read-only syntax viewer)
    object FileViewer : Screen("file_viewer?fileId={fileId}&projectId={projectId}", "Просмотр файла", Icons.Default.Visibility) {
        fun createRoute(fileId: Long, projectId: Long? = null): String {
            val p = projectId?.toString() ?: ""
            return "file_viewer?fileId=$fileId&projectId=$p"
        }
    }

    // 8. Code Editor
    object Editor : Screen("editor?fileId={fileId}&projectId={projectId}", "Редактор", Icons.Default.Code) {
        fun createRoute(fileId: Long? = null, projectId: Long? = null): String {
            val f = fileId?.toString() ?: ""
            val p = projectId?.toString() ?: ""
            return "editor?fileId=$f&projectId=$p"
        }
    }

    // 9. Code Review
    object CodeReview : Screen("review?projectId={projectId}", "Code Review", Icons.Default.Analytics) {
        fun createRoute(projectId: Long? = null): String = "review?projectId=${projectId ?: ""}"
    }

    // 10. AI Debugger
    object Debugger : Screen("debugger?projectId={projectId}", "AI Debugger", Icons.Default.BugReport) {
        fun createRoute(projectId: Long? = null): String = "debugger?projectId=${projectId ?: ""}"
    }

    // 11. Android Developer
    object AndroidDev : Screen("android_dev", "Android Developer", Icons.Default.Android)

    // 12. Project Architect
    object ProjectArchitect : Screen("architect?projectId={projectId}", "Project Architect", Icons.Default.AccountTree) {
        fun createRoute(projectId: Long? = null): String = "architect?projectId=${projectId ?: ""}"
    }

    // 13. Gemini Settings
    object GeminiSettings : Screen("gemini_settings", "Gemini AI", Icons.Default.Psychology)

    // 14. App Settings
    object Settings : Screen("settings", "Настройки", Icons.Default.Settings)

    // 15. About
    object About : Screen("about", "О приложении", Icons.Default.Info)

    // 16. Offline State (Status screen)
    object OfflineState : Screen("offline_state", "Автономный режим", Icons.Default.WifiOff)

    // 17. Error State (Status & diagnostics screen)
    object ErrorState : Screen("error_state", "Диагностика ошибок", Icons.Default.Warning)

    // Helper Generator route
    object Generator : Screen("generator", "Генератор", Icons.Default.AutoAwesome)
}

val BottomNavItems = listOf(
    Screen.Chat,
    Screen.Chats,
    Screen.Projects,
    Screen.Editor,
    Screen.AndroidDev
)

val DrawerNavItems = listOf(
    Screen.Chats,
    Screen.Projects,
    Screen.Editor,
    Screen.CodeReview,
    Screen.AndroidDev,
    Screen.ProjectArchitect,
    Screen.Settings,
    Screen.About
)
