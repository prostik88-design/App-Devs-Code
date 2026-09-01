package com.example.ui.navigation

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.about.AboutScreen
import com.example.ui.screens.androiddev.AndroidDeveloperScreen
import com.example.ui.screens.architect.ProjectArchitectScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.chats.ChatDetailScreen
import com.example.ui.screens.chats.ChatsListScreen
import com.example.ui.screens.debugger.DebuggerScreen
import com.example.ui.screens.editor.CodeEditorScreen
import com.example.ui.screens.editor.FileViewerScreen
import com.example.ui.screens.generator.AppGeneratorScreen
import com.example.ui.screens.projects.ProjectDetailOverviewScreen
import com.example.ui.screens.projects.ProjectFilesScreen
import com.example.ui.screens.projects.ProjectsScreen
import com.example.ui.screens.review.CodeReviewScreen
import com.example.ui.screens.settings.GeminiSettingsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.states.ErrorStateScreen
import com.example.ui.screens.states.OfflineStateScreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Code,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Devs Code",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text(
                                text = "AI Software Engineer",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }

                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    val allDrawerScreens = listOf(
                        Screen.Chat,
                        Screen.Chats,
                        Screen.Projects,
                        Screen.Editor,
                        Screen.CodeReview,
                        Screen.Debugger,
                        Screen.AndroidDev,
                        Screen.ProjectArchitect,
                        Screen.Settings,
                        Screen.About
                    )

                    allDrawerScreens.forEach { screen ->
                        val isSelected = currentRoute?.startsWith(screen.route.substringBefore("?").substringBefore("/")) == true
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) NeonCyan else TextSecondaryDark
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Chat.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier
                                .padding(NavigationDrawerItemDefaults.ItemPadding)
                                .testTag("drawer_item_${screen.route.substringBefore("?")}")
                        )
                    }
                }

                // Footer
                Text(
                    text = "Devs Code Studio v1.0\nNative Kotlin & Jetpack Compose",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark.copy(alpha = 0.6f),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    BottomNavItems.forEach { screen ->
                        val isSelected = currentRoute?.startsWith(screen.route.substringBefore("?").substringBefore("/")) == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) NeonCyan else TextSecondaryDark
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Chat.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.testTag("bottom_nav_${screen.route.substringBefore("?")}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Chat.route
                ) {
                    // 1. Home / AI Chat
                    composable(
                        route = Screen.Chat.route,
                        arguments = listOf(
                            navArgument("chatId") {
                                type = NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) {
                        ChatScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenSettings = { navController.navigate(Screen.Settings.route) }
                        )
                    }

                    // 2. Chats (History & Sessions)
                    composable(Screen.Chats.route) {
                        ChatsListScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onSelectChat = { chatId ->
                                navController.navigate(Screen.Chat.createRoute(chatId))
                            },
                            onOpenChatDetails = { chatId ->
                                navController.navigate(Screen.ChatDetail.createRoute(chatId))
                            }
                        )
                    }

                    // 3. Chat Details
                    composable(
                        route = Screen.ChatDetail.route,
                        arguments = listOf(navArgument("chatId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
                        ChatDetailScreen(
                            chatId = chatId,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenChat = { id ->
                                navController.navigate(Screen.Chat.createRoute(id))
                            }
                        )
                    }

                    // 4. My Projects
                    composable(Screen.Projects.route) {
                        ProjectsScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onSelectProject = { projectId ->
                                navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                            }
                        )
                    }

                    // 5. Project Details (Overview & Architecture)
                    composable(
                        route = Screen.ProjectDetail.route,
                        arguments = listOf(navArgument("projectId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
                        ProjectDetailOverviewScreen(
                            projectId = projectId,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenFiles = { id ->
                                navController.navigate(Screen.ProjectFiles.createRoute(id))
                            },
                            onOpenFile = { fileId ->
                                navController.navigate(Screen.FileViewer.createRoute(fileId = fileId, projectId = projectId))
                            },
                            onOpenReview = { id ->
                                navController.navigate(Screen.CodeReview.createRoute(id))
                            },
                            onOpenArchitect = { id ->
                                navController.navigate(Screen.ProjectArchitect.createRoute(id))
                            },
                            onOpenChat = { id ->
                                navController.navigate(Screen.Chat.createRoute())
                            }
                        )
                    }

                    // 6. Project Files Explorer
                    composable(
                        route = Screen.ProjectFiles.route,
                        arguments = listOf(navArgument("projectId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
                        ProjectFilesScreen(
                            projectId = projectId,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenFileInEditor = { fileId ->
                                navController.navigate(Screen.Editor.createRoute(fileId = fileId, projectId = projectId))
                            },
                            onOpenFileInViewer = { fileId ->
                                navController.navigate(Screen.FileViewer.createRoute(fileId = fileId, projectId = projectId))
                            },
                            onSendFileToChat = { fileName, content ->
                                navController.navigate(Screen.Chat.route)
                            }
                        )
                    }

                    // 7. File Viewer (Read-only)
                    composable(
                        route = Screen.FileViewer.route,
                        arguments = listOf(
                            navArgument("fileId") { type = NavType.StringType; defaultValue = "" },
                            navArgument("projectId") { type = NavType.StringType; defaultValue = "" }
                        )
                    ) { backStackEntry ->
                        val fileId = backStackEntry.arguments?.getString("fileId")?.toLongOrNull() ?: 0L
                        val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull()
                        FileViewerScreen(
                            fileId = fileId,
                            projectId = projectId,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenEditor = { id ->
                                navController.navigate(Screen.Editor.createRoute(fileId = id, projectId = projectId))
                            },
                            onSendToChat = { name, code ->
                                navController.navigate(Screen.Chat.route)
                            }
                        )
                    }

                    // 8. Code Editor
                    composable(
                        route = Screen.Editor.route,
                        arguments = listOf(
                            navArgument("fileId") { type = NavType.StringType; defaultValue = "" },
                            navArgument("projectId") { type = NavType.StringType; defaultValue = "" }
                        )
                    ) { backStackEntry ->
                        val fileIdStr = backStackEntry.arguments?.getString("fileId")
                        val projectIdStr = backStackEntry.arguments?.getString("projectId")
                        val fileId = fileIdStr?.toLongOrNull()
                        val projectId = projectIdStr?.toLongOrNull()
                        CodeEditorScreen(
                            fileId = fileId,
                            projectId = projectId,
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }

                    // 9. Code Review
                    composable(
                        route = Screen.CodeReview.route,
                        arguments = listOf(navArgument("projectId") { type = NavType.StringType; defaultValue = "" })
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull()
                        CodeReviewScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }

                    // 10. AI Debugger
                    composable(
                        route = Screen.Debugger.route,
                        arguments = listOf(navArgument("projectId") { type = NavType.StringType; defaultValue = "" })
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull()
                        DebuggerScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }

                    // 11. Android Developer
                    composable(Screen.AndroidDev.route) {
                        AndroidDeveloperScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }

                    // 12. Project Architect
                    composable(
                        route = Screen.ProjectArchitect.route,
                        arguments = listOf(navArgument("projectId") { type = NavType.StringType; defaultValue = "" })
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull()
                        ProjectArchitectScreen(
                            projectId = projectId,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNavigateToChat = { prompt ->
                                navController.navigate(Screen.Chat.route)
                            }
                        )
                    }

                    // 13. Gemini Settings
                    composable(Screen.GeminiSettings.route) {
                        GeminiSettingsScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // 14. App Settings
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNavigateToGeminiSettings = { navController.navigate(Screen.GeminiSettings.route) },
                            onNavigateToAbout = { navController.navigate(Screen.About.route) },
                            onNavigateToOffline = { navController.navigate(Screen.OfflineState.route) },
                            onNavigateToErrorState = { navController.navigate(Screen.ErrorState.route) }
                        )
                    }

                    // 15. About
                    composable(Screen.About.route) {
                        AboutScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    // 16. Offline State
                    composable(Screen.OfflineState.route) {
                        OfflineStateScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onOpenProjects = { navController.navigate(Screen.Projects.route) },
                            onOpenEditor = { navController.navigate(Screen.Editor.route) }
                        )
                    }

                    // 17. Error State
                    composable(Screen.ErrorState.route) {
                        ErrorStateScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateHome = { navController.navigate(Screen.Chat.route) },
                            onOpenSettings = { navController.navigate(Screen.GeminiSettings.route) },
                            onRetry = { navController.popBackStack() }
                        )
                    }

                    // Helper App Generator
                    composable(Screen.Generator.route) {
                        AppGeneratorScreen(
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenProject = { projectId ->
                                navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                            }
                        )
                    }
                }
            }
        }
    }
}
