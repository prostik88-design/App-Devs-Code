package com.example.ui.screens.editor

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MarkdownViewer
import com.example.ui.theme.CodeBgDark
import com.example.ui.theme.EditorGutterDark
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.util.ProjectExportUtils
import com.example.util.SafFileManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    fileId: Long? = null,
    projectId: Long? = null,
    onOpenDrawer: () -> Unit = {},
    viewModel: CodeEditorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    var showAiSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showCustomAiDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showFileInfoDialog by remember { mutableStateOf(false) }
    var customAiPrompt by remember { mutableStateOf("") }

    // SAF Document Creator (ACTION_CREATE_DOCUMENT)
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(uiState.content.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Файл успешно сохранен на устройство!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка сохранения: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // SAF Document Picker (ACTION_OPEN_DOCUMENT)
    val openSafFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val pId = projectId ?: uiState.currentFile?.projectId ?: 1L
            viewModel.openExternalSafUri(context, uri, pId)
        }
    }

    LaunchedEffect(fileId, projectId) {
        if (fileId != null && fileId > 0) {
            viewModel.loadFile(fileId, projectId)
        } else if (uiState.projectFiles.isNotEmpty() && uiState.currentFileId == null) {
            viewModel.loadFile(uiState.projectFiles.first().id, projectId)
        }
    }

    val lines = remember(uiState.content) { uiState.content.lines() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.currentFile?.name ?: "Редактор кода",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (!uiState.isSaved) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("•", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            text = uiState.currentFile?.relativePath ?: "Файл не выбран",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("editor_drawer_btn")) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                },
                actions = {
                    // Undo
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Отменить", tint = TextSecondaryDark)
                    }
                    // Redo
                    IconButton(onClick = { viewModel.redo() }) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Повторить", tint = TextSecondaryDark)
                    }
                    // Save
                    IconButton(
                        onClick = {
                            viewModel.saveCurrentFile()
                            Toast.makeText(context, "Файл сохранен", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("save_file_btn")
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Сохранить",
                            tint = if (uiState.isSaved) TextSecondaryDark else EmeraldGreen
                        )
                    }
                    // Ask AI
                    IconButton(
                        onClick = { showAiSheet = true },
                        modifier = Modifier.testTag("ask_ai_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Спросить AI", tint = NeonCyan)
                    }
                    // More options
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Настройки редактора")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Информация о файле") },
                                onClick = {
                                    showFileInfoDialog = true
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan) }
                            )
                            DropdownMenuItem(
                                text = { Text("Переименовать файл") },
                                onClick = {
                                    showRenameDialog = true
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Открыть файл с устройства (SAF)") },
                                onClick = {
                                    openSafFileLauncher.launch(arrayOf("*/*"))
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null, tint = EmeraldGreen) }
                            )
                            DropdownMenuItem(
                                text = { Text("Поиск и замена") },
                                onClick = {
                                    viewModel.toggleSearch()
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Номера строк: " + if (uiState.showLineNumbers) "Вкл ✓" else "Выкл") },
                                onClick = {
                                    viewModel.toggleLineNumbers()
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Перенос строк: " + if (uiState.wordWrap) "Вкл ✓" else "Выкл") },
                                onClick = {
                                    viewModel.toggleWordWrap()
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.WrapText, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Шрифт: ${uiState.fontSize}sp (Переключить)") },
                                onClick = {
                                    val nextSize = when (uiState.fontSize) {
                                        12 -> 14
                                        14 -> 16
                                        16 -> 18
                                        else -> 12
                                    }
                                    viewModel.setFontSize(nextSize)
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.FormatSize, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Скопировать код") },
                                onClick = {
                                    ProjectExportUtils.copyToClipboard(
                                        context,
                                        uiState.currentFile?.name ?: "code",
                                        uiState.content
                                    )
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Сохранить файл на диск (SAF)") },
                                onClick = {
                                    val fileName = uiState.currentFile?.name ?: "file.kt"
                                    saveFileLauncher.launch(fileName)
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = null, tint = NeonCyan) }
                            )
                            DropdownMenuItem(
                                text = { Text("Поделиться кодом") },
                                onClick = {
                                    ProjectExportUtils.shareText(
                                        context,
                                        uiState.currentFile?.name ?: "Devs Code Snippet",
                                        uiState.content
                                    )
                                    showMoreMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )
                        }
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
                .background(CodeBgDark)
        ) {
            // Search & Replace Bar
            AnimatedVisibility(visible = uiState.isSearchVisible) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = EditorGutterDark,
                    tonalElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Поиск...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                textStyle = TextStyle(fontSize = 12.sp, color = TextPrimaryDark)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedTextField(
                                value = uiState.replaceQuery,
                                onValueChange = { viewModel.setReplaceQuery(it) },
                                placeholder = { Text("Заменить на...", fontSize = 12.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                textStyle = TextStyle(fontSize = 12.sp, color = TextPrimaryDark)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = { viewModel.replaceAll() }) {
                                Icon(Icons.Default.FindReplace, contentDescription = "Заменить все", tint = NeonCyan)
                            }
                            IconButton(onClick = { viewModel.toggleSearch() }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondaryDark)
                            }
                        }
                    }
                }
            }

            // AI Loading Banner
            if (uiState.isAiProcessing) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Devs Code выполняет анализ кода с помощью Gemini...", fontSize = 12.sp)
                    }
                }
            }

            // AI Proposed Diff Banner
            AnimatedVisibility(visible = uiState.proposedAiContent != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI предлагает обновленный код",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Вы можете просмотреть предложенные изменения и принять их в файл.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = { viewModel.rejectProposedChanges() }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Отклонить")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.acceptProposedChanges() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Принять изменения")
                            }
                        }
                    }
                }
            }

            // AI Explanation Banner if available
            AnimatedVisibility(visible = !uiState.aiExplanation.isNullOrBlank() && uiState.proposedAiContent == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ответ AI Ассистента:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            IconButton(
                                onClick = { viewModel.rejectProposedChanges() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondaryDark)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        MarkdownViewer(content = uiState.aiExplanation!!)
                    }
                }
            }

            // Code Editor Area
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Line Numbers Gutter
                if (uiState.showLineNumbers) {
                    Column(
                        modifier = Modifier
                            .background(EditorGutterDark)
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                    ) {
                        lines.indices.forEach { index ->
                            Text(
                                text = "${index + 1}",
                                color = TextSecondaryDark.copy(alpha = 0.5f),
                                fontSize = uiState.fontSize.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = (uiState.fontSize + 6).sp
                            )
                        }
                    }
                }

                // Text Input Area
                val horizontalScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                        .then(if (!uiState.wordWrap) Modifier.horizontalScroll(horizontalScroll) else Modifier)
                ) {
                    OutlinedTextField(
                        value = uiState.content,
                        onValueChange = { viewModel.onContentChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("code_editor_textarea"),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = uiState.fontSize.sp,
                            color = TextPrimaryDark,
                            lineHeight = (uiState.fontSize + 6).sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }

    // Modal: Ask AI Sheet
    if (showAiSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Спросить AI об этом файле",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                CodeAction.values().forEach { action ->
                    Surface(
                        onClick = {
                            viewModel.executeAiAction(action)
                            showAiSheet = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = action.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Custom AI prompt button
                OutlinedButton(
                    onClick = {
                        showAiSheet = false
                        showCustomAiDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Свой запрос к коду...")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Dialog: Custom AI Prompt
    if (showCustomAiDialog) {
        AlertDialog(
            onDismissRequest = { showCustomAiDialog = false },
            title = { Text("Пользовательский запрос к коду") },
            text = {
                OutlinedTextField(
                    value = customAiPrompt,
                    onValueChange = { customAiPrompt = it },
                    placeholder = { Text("Например: «Перепиши этот DAO на Kotlin Flow и добавь пагинацию»") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customAiPrompt.isNotBlank()) {
                            viewModel.executeAiAction(CodeAction.REFACTOR, customAiPrompt)
                            showCustomAiDialog = false
                            customAiPrompt = ""
                        }
                    }
                ) {
                    Text("Отправить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomAiDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog: Rename File
    if (showRenameDialog && uiState.currentFile != null) {
        val currentFile = uiState.currentFile!!
        var newName by remember { mutableStateOf(currentFile.name) }
        var newRelPath by remember { mutableStateOf(currentFile.relativePath) }

        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
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
                            viewModel.renameCurrentFile(newName.trim(), newRelPath.trim())
                            showRenameDialog = false
                            Toast.makeText(context, "Файл переименован", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog: File Info / Metadata
    if (showFileInfoDialog && uiState.currentFile != null) {
        val currentFile = uiState.currentFile!!
        val ext = currentFile.name.substringAfterLast(".", "")
        val formattedSize = SafFileManager.formatFileSize(currentFile.fileSize)
        val modDate = dateFormat.format(Date(currentFile.updatedAt))

        AlertDialog(
            onDismissRequest = { showFileInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan)
                    Text("Свойства файла", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.example.ui.screens.projects.MetadataRow(label = "Имя файла", value = currentFile.name)
                    com.example.ui.screens.projects.MetadataRow(label = "Расширение", value = if (ext.isNotBlank()) ".$ext" else "нет")
                    com.example.ui.screens.projects.MetadataRow(label = "Язык", value = currentFile.language)
                    com.example.ui.screens.projects.MetadataRow(label = "Размер", value = "$formattedSize (${currentFile.fileSize} байт)")
                    com.example.ui.screens.projects.MetadataRow(label = "Путь в проекте", value = currentFile.relativePath)
                    com.example.ui.screens.projects.MetadataRow(label = "Изменен", value = modDate)
                    com.example.ui.screens.projects.MetadataRow(label = "Версия", value = "v${currentFile.version}")
                }
            },
            confirmButton = {
                Button(onClick = { showFileInfoDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}
