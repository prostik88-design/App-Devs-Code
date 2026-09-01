package com.example.ui.screens.projects

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.ProjectEntity
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onOpenDrawer: () -> Unit = {},
    onSelectProject: (Long) -> Unit = {},
    viewModel: ProjectsViewModel = viewModel()
) {
    val projects by viewModel.filteredProjects.collectAsState()
    val allProjects by viewModel.rawProjects.collectAsState()
    val currentTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val context = LocalContext.current
    var isSearchActive by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var projectToEdit by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var pendingExportZipProjectId by remember { mutableStateOf<Long?>(null) }

    // SAF Zip Document Creator
    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null && pendingExportZipProjectId != null) {
            viewModel.exportProjectToZipUri(context, pendingExportZipProjectId!!, uri)
        }
        pendingExportZipProjectId = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Поиск проектов...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Очистить")
                                    }
                                }
                            }
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Мои проекты",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${allProjects.size} проектов в хранилище",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("projects_drawer_btn")) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) viewModel.setSearchQuery("")
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Поиск"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("create_project_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Создать проект")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs: All / Active / Archived
            SecondaryTabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = currentTab == ProjectFilterTab.ALL,
                    onClick = { viewModel.setFilterTab(ProjectFilterTab.ALL) },
                    text = { Text("Все (${allProjects.size})") }
                )
                Tab(
                    selected = currentTab == ProjectFilterTab.ACTIVE,
                    onClick = { viewModel.setFilterTab(ProjectFilterTab.ACTIVE) },
                    text = { Text("Активные (${allProjects.count { !it.isArchived }})") }
                )
                Tab(
                    selected = currentTab == ProjectFilterTab.ARCHIVED,
                    onClick = { viewModel.setFilterTab(ProjectFilterTab.ARCHIVED) },
                    text = { Text("В архиве (${allProjects.count { it.isArchived }})") }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                // Quick Start Templates Carousel (shown when on ALL or ACTIVE tab and not searching)
                if (searchQuery.isBlank() && currentTab != ProjectFilterTab.ARCHIVED) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Быстрый старт и шаблоны",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Text(
                                    text = "5 готовых стеков",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 6.dp)
                            ) {
                                item {
                                    PresetTemplateCard(
                                        title = "Expense App",
                                        subtitle = "Android + Room",
                                        icon = Icons.Default.Paid,
                                        iconBg = Color(0xFF10B981),
                                        onClick = {
                                            viewModel.createPresetProject("expense_app")
                                            Toast.makeText(context, "Проект Expense App создан!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                                item {
                                    PresetTemplateCard(
                                        title = "Telegram Bot",
                                        subtitle = "Python + Gemini AI",
                                        icon = Icons.Default.SmartToy,
                                        iconBg = Color(0xFF0284C7),
                                        onClick = {
                                            viewModel.createPresetProject("telegram_bot")
                                            Toast.makeText(context, "Проект Telegram Bot создан!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                                item {
                                    PresetTemplateCard(
                                        title = "Website",
                                        subtitle = "Next.js + React 19",
                                        icon = Icons.Default.Language,
                                        iconBg = Color(0xFF8B5CF6),
                                        onClick = {
                                            viewModel.createPresetProject("website")
                                            Toast.makeText(context, "Проект Website создан!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                                item {
                                    PresetTemplateCard(
                                        title = "Game",
                                        subtitle = "Compose Canvas 2D",
                                        icon = Icons.Default.Games,
                                        iconBg = Color(0xFFF59E0B),
                                        onClick = {
                                            viewModel.createPresetProject("game")
                                            Toast.makeText(context, "Проект 2D Game создан!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                                item {
                                    PresetTemplateCard(
                                        title = "Android App",
                                        subtitle = "Kotlin + M3 Clean Arch",
                                        icon = Icons.Default.PhoneAndroid,
                                        iconBg = Color(0xFF06B6D4),
                                        onClick = {
                                            viewModel.createPresetProject("android_app")
                                            Toast.makeText(context, "Проект Android App создан!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (projects.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = TextSecondaryDark
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Ничего не найдено" else "Проектов в этом списке нет",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Попробуйте изменить поисковый запрос" else "Создайте новый проект или воспользуйтесь шаблонами выше",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryDark,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { showCreateDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Создать проект")
                                }
                            }
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectItemCard(
                            project = project,
                            onClick = { onSelectProject(project.id) },
                            onEdit = { projectToEdit = project },
                            onToggleArchive = { viewModel.toggleArchiveProject(project.id, project.isArchived) },
                            onDelete = { projectToDelete = project },
                            onExportZip = {
                                pendingExportZipProjectId = project.id
                                val safeName = project.name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "project" }
                                createZipLauncher.launch("${safeName}.zip")
                            },
                            onShareZip = {
                                viewModel.shareProjectZip(context, project.id)
                            },
                            onCopyCode = {
                                viewModel.copyAllProjectCode(context, project.id)
                            },
                            onShareText = {
                                viewModel.shareProjectMarkdown(context, project.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Create New Project
    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, platform, lang, framework, arch, status ->
                viewModel.createNewProject(
                    name = name,
                    description = desc,
                    platform = platform,
                    language = lang,
                    framework = framework,
                    architecture = arch,
                    status = status
                )
                showCreateDialog = false
                Toast.makeText(context, "Проект «$name» создан!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog: Edit Project
    projectToEdit?.let { project ->
        EditProjectDialog(
            project = project,
            onDismiss = { projectToEdit = null },
            onSave = { name, desc, platform, lang, framework, arch, status ->
                viewModel.updateProjectDetails(
                    project = project,
                    name = name,
                    description = desc,
                    platform = platform,
                    language = lang,
                    framework = framework,
                    architecture = arch,
                    status = status
                )
                projectToEdit = null
                Toast.makeText(context, "Параметры проекта обновлены", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog: Delete Project
    projectToDelete?.let { project ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Удалить проект «${project.name}»?") },
            text = { Text("Все связанные файлы, папки, чаты и отчеты будут удалены из базы данных безвозвратно.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(project.id)
                        projectToDelete = null
                        Toast.makeText(context, "Проект удален", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun PresetTemplateCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = iconBg.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconBg, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark,
                fontSize = 11.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan)
                Text(text = "Создать", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectItemCard(
    project: ProjectEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    onExportZip: () -> Unit,
    onShareZip: () -> Unit,
    onCopyCode: () -> Unit,
    onShareText: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("project_card_${project.id}"),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (project.isArchived) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (project.platform.lowercase()) {
                                "android" -> Icons.Default.PhoneAndroid
                                "web" -> Icons.Default.Language
                                "python", "backend" -> Icons.Default.SmartToy
                                "game" -> Icons.Default.Games
                                else -> Icons.Default.Folder
                            },
                            contentDescription = null,
                            tint = if (project.isArchived) TextSecondaryDark else NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (project.isArchived) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Архив",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Text(
                        text = "${project.platform} • ${project.language} • ${project.framework}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню проекта")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Открыть проект") },
                            onClick = {
                                onClick()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Экспорт в ZIP (Сохранить)") },
                            onClick = {
                                onExportZip()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.FolderZip, contentDescription = null, tint = NeonCyan) }
                        )
                        DropdownMenuItem(
                            text = { Text("Поделиться ZIP архивом") },
                            onClick = {
                                onShareZip()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Скопировать весь код") },
                            onClick = {
                                onCopyCode()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Поделиться текстом") },
                            onClick = {
                                onShareText()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Изменить параметры") },
                            onClick = {
                                onEdit()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (project.isArchived) "Разархивировать" else "В архив") },
                            onClick = {
                                onToggleArchive()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (project.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить проект", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            if (project.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges & Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusBadge(status = project.status)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = project.architecture,
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Обновлено: ${dateFormat.format(Date(project.updatedAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondaryDark,
                    fontSize = 10.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onExportZip, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.FolderZip, contentDescription = "ZIP", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onCopyCode, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onShareZip, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (label, bg, fg) = when (status.uppercase()) {
        "ACTIVE", "АКТИВНЫЙ" -> Triple("Активен", EmeraldGreen.copy(alpha = 0.15f), EmeraldGreen)
        "IN_PROGRESS", "В РАЗРАБОТКЕ" -> Triple("В разработке", NeonCyan.copy(alpha = 0.15f), NeonCyan)
        "COMPLETED", "ЗАВЕРШЕН" -> Triple("Завершён", Color(0xFF8B5CF6).copy(alpha = 0.15f), Color(0xFF8B5CF6))
        "ARCHIVED", "В АРХИВЕ" -> Triple("Архив", TextSecondaryDark.copy(alpha = 0.15f), TextSecondaryDark)
        else -> Triple(status, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            fontSize = 10.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, platform: String, lang: String, framework: String, arch: String, status: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("Android") }
    var language by remember { mutableStateOf("Kotlin") }
    var framework by remember { mutableStateOf("Jetpack Compose") }
    var architecture by remember { mutableStateOf("MVVM + Clean Architecture") }
    var status by remember { mutableStateOf("ACTIVE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый проект", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название проекта *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = platform,
                        onValueChange = { platform = it },
                        label = { Text("Платформа") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = language,
                        onValueChange = { language = it },
                        label = { Text("Язык") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = framework,
                        onValueChange = { framework = it },
                        label = { Text("Фреймворк") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = architecture,
                        onValueChange = { architecture = it },
                        label = { Text("Архитектура") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name, description, platform, language, framework, architecture, status)
                    }
                }
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectDialog(
    project: ProjectEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, platform: String, lang: String, framework: String, arch: String, status: String) -> Unit
) {
    var name by remember { mutableStateOf(project.name) }
    var description by remember { mutableStateOf(project.description) }
    var platform by remember { mutableStateOf(project.platform) }
    var language by remember { mutableStateOf(project.language) }
    var framework by remember { mutableStateOf(project.framework) }
    var architecture by remember { mutableStateOf(project.architecture) }
    var status by remember { mutableStateOf(project.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Параметры проекта", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название проекта *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = platform,
                        onValueChange = { platform = it },
                        label = { Text("Платформа") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = language,
                        onValueChange = { language = it },
                        label = { Text("Язык") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = framework,
                        onValueChange = { framework = it },
                        label = { Text("Фреймворк") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = architecture,
                        onValueChange = { architecture = it },
                        label = { Text("Архитектура") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Статус (ACTIVE / IN_PROGRESS / COMPLETED)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, description, platform, language, framework, architecture, status)
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
