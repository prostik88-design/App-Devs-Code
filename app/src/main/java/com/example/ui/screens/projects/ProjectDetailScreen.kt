package com.example.ui.screens.projects

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectFileEntity
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondaryDark
import com.example.util.ProjectExportUtils
import com.example.util.SafFileManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    onNavigateBack: () -> Unit = {},
    onOpenFile: (Long) -> Unit = {},
    onSendFileToChat: (String, String) -> Unit = { _, _ -> },
    viewModel: ProjectsViewModel = viewModel()
) {
    val project by viewModel.currentProject.collectAsState()
    val files by viewModel.selectedProjectFiles.collectAsState()
    val isImporting by viewModel.isImportingFiles.collectAsState()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    var showCreateFileDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<ProjectFileEntity?>(null) }
    var fileToRename by remember { mutableStateOf<ProjectFileEntity?>(null) }
    var fileInfoTarget by remember { mutableStateOf<ProjectFileEntity?>(null) }
    var showProjectMenu by remember { mutableStateOf(false) }
    var showEditProjectDialog by remember { mutableStateOf(false) }

    // SAF Zip Document Creator (ACTION_CREATE_DOCUMENT)
    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null && project != null) {
            viewModel.exportProjectToZipUri(context, project!!.id, uri)
        }
    }

    // SAF Single File Document Creator (ACTION_CREATE_DOCUMENT)
    var pendingSaveFile by remember { mutableStateOf<ProjectFileEntity?>(null) }
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null && pendingSaveFile != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(pendingSaveFile!!.content.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Файл «${pendingSaveFile!!.name}» сохранен!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка сохранения: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
        pendingSaveFile = null
    }

    // SAF Single File Picker (ACTION_OPEN_DOCUMENT)
    val pickSingleFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && project != null) {
            viewModel.importSingleSafFile(context, project!!.id, uri)
        }
    }

    // SAF Folder Tree Picker (ACTION_OPEN_DOCUMENT_TREE)
    val pickFolderTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null && project != null) {
            viewModel.importFolderSafTree(context, project!!.id, treeUri)
        }
    }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project?.name ?: "Проект",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${files.size} файлов • ${project?.platform ?: "Devs Code"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Export ZIP
                    IconButton(
                        onClick = {
                            project?.let { p ->
                                val safeName = p.name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "project" }
                                createZipLauncher.launch("${safeName}.zip")
                            }
                        }
                    ) {
                        Icon(Icons.Default.FolderZip, contentDescription = "Экспорт ZIP", tint = NeonCyan)
                    }

                    // Share ZIP
                    IconButton(
                        onClick = {
                            project?.let { p ->
                                viewModel.shareProjectZip(context, p.id)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться архивом")
                    }

                    // Copy all code
                    IconButton(
                        onClick = {
                            project?.let { p ->
                                viewModel.copyAllProjectCode(context, p.id)
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать весь код")
                    }

                    // Add file
                    IconButton(onClick = { showCreateFileDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить файл", tint = EmeraldGreen)
                    }

                    // More Menu
                    Box {
                        IconButton(onClick = { showProjectMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Опции проекта")
                        }
                        DropdownMenu(
                            expanded = showProjectMenu,
                            onDismissRequest = { showProjectMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Импорт файла (SAF)") },
                                onClick = {
                                    pickSingleFileLauncher.launch(arrayOf("*/*"))
                                    showProjectMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null, tint = NeonCyan) }
                            )
                            DropdownMenuItem(
                                text = { Text("Импорт папки (SAF Tree)") },
                                onClick = {
                                    pickFolderTreeLauncher.launch(null)
                                    showProjectMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.DriveFolderUpload, contentDescription = null, tint = EmeraldGreen) }
                            )
                            DropdownMenuItem(
                                text = { Text("Экспорт Markdown") },
                                onClick = {
                                    project?.let { viewModel.shareProjectMarkdown(context, it.id) }
                                    showProjectMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Параметры проекта") },
                                onClick = {
                                    showEditProjectDialog = true
                                    showProjectMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (project?.isArchived == true) "Разархивировать" else "В архив") },
                                onClick = {
                                    project?.let { viewModel.toggleArchiveProject(it.id, it.isArchived) }
                                    showProjectMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        if (project?.isArchived == true) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateFileDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить файл")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Loading SAF banner
            if (isImporting) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(
                                "Импорт файлов через Storage Access Framework...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Project Header Info Card
            project?.let { p ->
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(status = p.status)
                                Text(
                                    text = "Создан: ${dateFormat.format(Date(p.createdAt))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp
                                )
                            }

                            if (p.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = p.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TechBadge("Платформа", p.platform)
                                TechBadge("Язык", p.language)
                                TechBadge("Стек", p.framework)
                                TechBadge("Архитектура", p.architecture)
                            }
                        }
                    }
                }
            }

            // SAF Action Toolbar Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickSingleFileLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Импорт файла", fontSize = 12.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { pickFolderTreeLauncher.launch(null) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DriveFolderUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Импорт папки", fontSize = 12.sp, maxLines = 1)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Файлы проекта (${files.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    TextButton(onClick = { showCreateFileDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Создать файл")
                    }
                }
            }

            if (files.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = TextSecondaryDark
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "В проекте нет файлов",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Создайте новый файл или импортируйте файлы через SAF",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { showCreateFileDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Создать")
                                }
                                OutlinedButton(onClick = { pickSingleFileLauncher.launch(arrayOf("*/*")) }) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Импорт SAF")
                                }
                            }
                        }
                    }
                }
            } else {
                items(files, key = { it.id }) { file ->
                    ProjectFileItem(
                        file = file,
                        onClick = { onOpenFile(file.id) },
                        onSendToAi = { onSendFileToChat(file.name, file.content) },
                        onCopyCode = { ProjectExportUtils.copyToClipboard(context, file.name, file.content) },
                        onSaveFile = {
                            pendingSaveFile = file
                            saveFileLauncher.launch(file.name)
                        },
                        onShareFile = {
                            ProjectExportUtils.shareText(context, "Файл: ${file.name}", file.content)
                        },
                        onShowInfo = { fileInfoTarget = file },
                        onRename = { fileToRename = file },
                        onDelete = { fileToDelete = file }
                    )
                }
            }
        }
    }

    // Dialog: Create File
    if (showCreateFileDialog) {
        var fileName by remember { mutableStateOf("") }
        var relativePath by remember { mutableStateOf("") }
        var initialCode by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Создать новый файл", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = {
                            fileName = it
                            if (relativePath.isBlank() || relativePath.endsWith("/")) {
                                val folder = when {
                                    it.endsWith(".kt") || it.endsWith(".java") -> "app/src/main/java/com/example/"
                                    it.endsWith(".xml") -> "app/src/main/res/layout/"
                                    it.endsWith(".py") -> ""
                                    it.endsWith(".tsx") || it.endsWith(".ts") -> "src/"
                                    else -> ""
                                }
                                relativePath = folder + it
                            }
                        },
                        label = { Text("Имя файла (напр. UserRepository.kt)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = relativePath,
                        onValueChange = { relativePath = it },
                        label = { Text("Относительный путь") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = initialCode,
                        onValueChange = { initialCode = it },
                        label = { Text("Начальный код (опционально)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        maxLines = 7
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileName.isNotBlank()) {
                            val ext = fileName.substringAfterLast(".", "kt")
                            viewModel.createFile(
                                projectId = projectId,
                                name = fileName,
                                relativePath = relativePath.ifBlank { fileName },
                                language = ext,
                                content = initialCode
                            )
                            showCreateFileDialog = false
                            Toast.makeText(context, "Файл «$fileName» создан!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog: Rename File
    fileToRename?.let { file ->
        var newName by remember { mutableStateOf(file.name) }
        var newRelPath by remember { mutableStateOf(file.relativePath) }

        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Переименовать файл", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            if (newRelPath.contains("/")) {
                                val parent = newRelPath.substringBeforeLast("/")
                                newRelPath = "$parent/$it"
                            } else {
                                newRelPath = it
                            }
                        },
                        label = { Text("Имя файла") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newRelPath,
                        onValueChange = { newRelPath = it },
                        label = { Text("Относительный путь") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renameFile(file.id, newName.trim(), newRelPath.trim())
                            fileToRename = null
                            Toast.makeText(context, "Файл переименован", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog: File Info / Metadata
    fileInfoTarget?.let { file ->
        val ext = file.name.substringAfterLast(".", "")
        val formattedSize = SafFileManager.formatFileSize(file.fileSize)
        val modDate = dateFormat.format(Date(file.updatedAt))

        AlertDialog(
            onDismissRequest = { fileInfoTarget = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan)
                    Text("Метаданные файла", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetadataRow(label = "Имя файла", value = file.name)
                    MetadataRow(label = "Расширение", value = if (ext.isNotBlank()) ".$ext" else "нет")
                    MetadataRow(label = "Язык", value = file.language)
                    MetadataRow(label = "Размер", value = "$formattedSize (${file.fileSize} байт)")
                    MetadataRow(label = "Путь в проекте", value = file.relativePath)
                    MetadataRow(label = "Изменен", value = modDate)
                    MetadataRow(label = "Версия", value = "v${file.version}")
                }
            },
            confirmButton = {
                Button(onClick = { fileInfoTarget = null }) {
                    Text("Закрыть")
                }
            }
        )
    }

    // Dialog: Delete File
    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Удалить файл «${file.name}»?") },
            text = { Text("Файл `${file.relativePath}` будет безвозвратно удален.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(file.id)
                        fileToDelete = null
                        Toast.makeText(context, "Файл удален", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog: Edit Project
    if (showEditProjectDialog && project != null) {
        EditProjectDialog(
            project = project!!,
            onDismiss = { showEditProjectDialog = false },
            onSave = { name, desc, platform, lang, framework, arch, status ->
                viewModel.updateProjectDetails(
                    project = project!!,
                    name = name,
                    description = desc,
                    platform = platform,
                    language = lang,
                    framework = framework,
                    architecture = arch,
                    status = status
                )
                showEditProjectDialog = false
                Toast.makeText(context, "Параметры обновлены", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryDark,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TechBadge(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "$label: ", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, fontSize = 10.sp)
            Text(text = value, style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
        }
    }
}

@Composable
fun ProjectFileItem(
    file: ProjectFileEntity,
    onClick: () -> Unit,
    onSendToAi: () -> Unit,
    onCopyCode: () -> Unit,
    onSaveFile: () -> Unit,
    onShareFile: () -> Unit,
    onShowInfo: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("file_row_${file.id}"),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (file.language.lowercase()) {
                            "kt", "kotlin", "java", "py", "python", "js", "ts", "tsx" -> Icons.Default.Code
                            else -> Icons.Default.Description
                        },
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SafFileManager.formatFileSize(file.fileSize),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldGreen,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        fontSize = 10.sp
                    )
                    Text(
                        text = file.relativePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = onSendToAi) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "В AI чат",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Опции файла",
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Информация / Свойства") },
                        onClick = {
                            onShowInfo()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan) }
                    )
                    DropdownMenuItem(
                        text = { Text("Переименовать") },
                        onClick = {
                            onRename()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Копировать код") },
                        onClick = {
                            onCopyCode()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Сохранить на диск (SAF)") },
                        onClick = {
                            onSaveFile()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Поделиться") },
                        onClick = {
                            onShareFile()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}
