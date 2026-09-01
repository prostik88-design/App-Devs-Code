package com.example.ui.screens.chats

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.ChatSessionEntity
import com.example.ui.screens.states.EmptyStateView
import com.example.ui.screens.states.LoadingStateView
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    onOpenDrawer: () -> Unit = {},
    onSelectChat: (Long) -> Unit = {},
    onOpenChatDetails: (Long) -> Unit = {},
    viewModel: ChatsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    var showNewChatDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var targetChat by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var newChatTitle by remember { mutableStateOf("") }
    var renameChatTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История диалогов", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("chats_drawer_btn")) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                },
                actions = {
                    IconButton(onClick = { showNewChatDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Новый диалог", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_new_chat_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Новый чат")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Поиск по чатам...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.isLoading) {
                LoadingStateView(message = "Загрузка списка диалогов...")
            } else if (uiState.filteredChats.isEmpty()) {
                EmptyStateView(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = if (uiState.searchQuery.isNotEmpty()) "Ничего не найдено" else "Нет сохраненных чатов",
                    description = if (uiState.searchQuery.isNotEmpty()) "Попробуйте изменить поисковый запрос." else "Создайте новый диалог, чтобы начать разработку с AI.",
                    actionLabel = "Создать новый диалог",
                    onActionClick = { showNewChatDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredChats, key = { it.id }) { chat ->
                        var showChatOptions by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectChat(chat.id) }
                                .testTag("chat_card_${chat.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chat.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = dateFormat.format(Date(chat.updatedAt)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryDark,
                                            fontSize = 11.sp
                                        )
                                        if (chat.projectId != null) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "Проект #${chat.projectId}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = NeonCyan,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Box {
                                    IconButton(
                                        onClick = { showChatOptions = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Опции")
                                    }

                                    DropdownMenu(
                                        expanded = showChatOptions,
                                        onDismissRequest = { showChatOptions = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Открыть чат") },
                                            onClick = {
                                                showChatOptions = false
                                                onSelectChat(chat.id)
                                            },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Детали и статистика") },
                                            onClick = {
                                                showChatOptions = false
                                                onOpenChatDetails(chat.id)
                                            },
                                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Переименовать") },
                                            onClick = {
                                                showChatOptions = false
                                                targetChat = chat
                                                renameChatTitle = chat.title
                                                showRenameDialog = true
                                            },
                                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Удалить диалог", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                showChatOptions = false
                                                targetChat = chat
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

    // Dialog: Create New Chat
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Новый AI диалог", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Введите тему или рабочее название для нового диалога:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newChatTitle,
                        onValueChange = { newChatTitle = it },
                        placeholder = { Text("Например: Разработка Compose UI") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = if (newChatTitle.isNotBlank()) newChatTitle.trim() else "Новый диалог"
                        viewModel.createNewChat(title = title) { newId ->
                            showNewChatDialog = false
                            newChatTitle = ""
                            onSelectChat(newId)
                        }
                    }
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog: Rename Chat
    if (showRenameDialog && targetChat != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Переименовать диалог", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameChatTitle,
                    onValueChange = { renameChatTitle = it },
                    label = { Text("Название диалога") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameChatTitle.isNotBlank()) {
                            viewModel.renameChat(targetChat!!.id, renameChatTitle.trim())
                            showRenameDialog = false
                            Toast.makeText(context, "Диалог переименован", Toast.LENGTH_SHORT).show()
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

    // Dialog: Delete Confirm
    if (showDeleteConfirmDialog && targetChat != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Удалить диалог?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Вы уверены, что хотите удалить диалог «${targetChat!!.title}»? Все сообщения и ответы AI будут удалены без возможности восстановления.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChat(targetChat!!.id)
                        showDeleteConfirmDialog = false
                        Toast.makeText(context, "Диалог удален", Toast.LENGTH_SHORT).show()
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
