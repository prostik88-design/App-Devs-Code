package com.example.ui.screens.projects

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.ProjectFileEntity
import com.example.ui.screens.states.EmptyStateView
import com.example.ui.screens.states.LoadingStateView
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondaryDark
import com.example.util.ProjectExportUtils
import com.example.util.SafFileManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFilesScreen(
    projectId: Long,
    onNavigateBack: () -> Unit = {},
    onOpenFileInEditor: (Long) -> Unit = {},
    onOpenFileInViewer: (Long) -> Unit = {},
    onSendFileToChat: (String, String) -> Unit = { _, _ -> },
    viewModel: ProjectsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var targetFile by remember { mutableStateOf<ProjectFileEntity?>(null) }
    var newFileName by remember { mutableStateOf("") }
    var newFileRelPath by remember { mutableStateOf("") }
    var newFileContent by remember { mutableStateOf("") }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    // SAF Import Single File
    val openSafFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importSingleSafFile(context, projectId, uri)
        }
    }

    // SAF Import Folder Tree
    val openSafFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importFolderSafTree(context, projectId, uri)
        }
    }

    val project = uiState.selectedProject
    val files = uiState.projectFiles

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) || it.relativePath.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${project?.name ?: "Проект"} — Файлы",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Файловый менеджер (${files.size} файлов)",
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
                    IconButton(onClick = { openSafFileLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.DriveFolderUpload, contentDescription = "Импорт SAF файла", tint = NeonCyan)
                    }
                    IconButton(onClick = { showNewFileDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Создать файл")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewFileDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_file_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Создать файл")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Поиск файлов...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.isLoading && files.isEmpty()) {
                LoadingStateView(message = "Загрузка списка файлов...")
            } else if (filteredFiles.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Description,
                    title = if (searchQuery.isNotEmpty()) "Файлы не найдены" else "В проекте нет файлов",
                    description = if (searchQuery.isNotEmpty()) "Попробуйте другой поисковый запрос." else "Создайте новый исходный файл или импортируйте дерево файлов с устройства (SAF).",
                    actionLabel = "Создать файл",
                    onActionClick = { showNewFileDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFiles, key = { it.id }) { file ->
                        var showFileOptions by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenFileInEditor(file.id) }
                                .testTag("file_item_${file.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(file.relativePath, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark, fontSize = 11.sp)
                                    Text(
                                        "${SafFileManager.formatFileSize(file.fileSize)} • ${file.language}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark,
                                        fontSize = 10.sp
                                    )
                                }

                                IconButton(onClick = { onOpenFileInViewer(file.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Visibility, contentDescription = "Просмотр", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                                }

                                Box {
                                    IconButton(onClick = { showFileOptions = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Опции", modifier = Modifier.size(18.dp))
                                    }

                                    DropdownMenu(
                                        expanded = showFileOptions,
                                        onDismissRequest = { showFileOptions = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Открыть в редакторе") },
                                            onClick = {
                                                showFileOptions = false
                                                onOpenFileInEditor(file.id)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Просмотреть код") },
                                            onClick = {
                                                showFileOptions = false
                                                onOpenFileInViewer(file.id)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Отправить в AI Чат") },
                                            onClick = {
                                                showFileOptions = false
                                                onSendFileToChat(file.name, file.content)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, tint = NeonCyan) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Скопировать код") },
                                            onClick = {
                                                showFileOptions = false
                                                ProjectExportUtils.copyToClipboard(context, file.name, file.content)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Удалить файл", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                showFileOptions = false
                                                targetFile = file
                                                showDeleteConfirmDialog = true
                                            },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Dialog: Create New File
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Создать файл", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = {
                            newFileName = it
                            if (newFileRelPath.isBlank() || !newFileRelPath.contains("/")) {
                                newFileRelPath = "src/$it"
                            }
                        },
                        label = { Text("Имя файла (с расширением)") },
                        placeholder = { Text("MainActivity.kt") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newFileRelPath,
                        onValueChange = { newFileRelPath = it },
                        label = { Text("Относительный путь") },
                        placeholder = { Text("src/main/java/MainActivity.kt") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createFile(
                                projectId = projectId,
                                name = newFileName.trim(),
                                relativePath = if (newFileRelPath.isNotBlank()) newFileRelPath.trim() else newFileName.trim(),
                                language = newFileName.substringAfterLast(".", "kt"),
                                content = "// Created in Devs Code Studio\n"
                            )
                            showNewFileDialog = false
                            newFileName = ""
                            newFileRelPath = ""
                            Toast.makeText(context, "Файл успешно создан", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog: Delete Confirm
    if (showDeleteConfirmDialog && targetFile != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Удалить файл?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Вы уверены, что хотите удалить файл «${targetFile!!.name}»? Это действие необратимо.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(targetFile!!.id)
                        showDeleteConfirmDialog = false
                        Toast.makeText(context, "Файл удален", Toast.LENGTH_SHORT).show()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
