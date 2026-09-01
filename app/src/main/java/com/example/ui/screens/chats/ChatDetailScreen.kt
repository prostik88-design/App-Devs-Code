package com.example.ui.screens.chats

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.components.MarkdownViewer
import com.example.ui.screens.states.EmptyStateView
import com.example.ui.screens.states.LoadingStateView
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextSecondaryDark
import com.example.util.ProjectExportUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: Long,
    onNavigateBack: () -> Unit = {},
    onOpenChat: (Long) -> Unit = {},
    viewModel: ChatsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(chatId) {
        viewModel.loadChatDetails(chatId)
    }

    val selectedChat = uiState.selectedChat
    val messages = uiState.currentMessages

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedChat?.title ?: "Детали диалога",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "Статистика и история сообщений",
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
                    IconButton(
                        onClick = {
                            val transcript = buildString {
                                append("# Диалог: ${selectedChat?.title ?: "Devs Code Session"}\n\n")
                                messages.forEach { msg ->
                                    val sender = if (msg.isFromUser) "👤 Пользователь" else "🤖 Devs Code AI"
                                    append("### $sender (${dateFormat.format(Date(msg.timestamp))})\n\n")
                                    append(msg.content)
                                    append("\n\n---\n\n")
                                }
                            }
                            ProjectExportUtils.shareText(context, selectedChat?.title ?: "Chat Transcript", transcript)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться стенограммой")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading && selectedChat == null) {
            LoadingStateView(message = "Загрузка параметров диалога...")
        } else if (selectedChat == null) {
            EmptyStateView(
                title = "Диалог не найден",
                description = "Возможно диалог был удален или перенесен.",
                actionLabel = "Вернуться назад",
                onActionClick = onNavigateBack
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Chat Overview Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Свойства диалога",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Button(
                                    onClick = { onOpenChat(chatId) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Перейти к чату")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Всего сообщений", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                                    Text("${messages.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Привязанный проект", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                                    Text(
                                        if (selectedChat.projectId != null) "Проект #${selectedChat.projectId}" else "Общий",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Дата создания", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                                    Text(dateFormat.format(Date(selectedChat.createdAt)), style = MaterialTheme.typography.bodySmall)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Последняя активность", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                                    Text(dateFormat.format(Date(selectedChat.updatedAt)), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "История сообщений (${messages.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = {
                                val transcript = buildString {
                                    messages.forEach { msg ->
                                        val sender = if (msg.isFromUser) "User" else "Devs Code AI"
                                        append("$sender: ${msg.content}\n\n")
                                    }
                                }
                                ProjectExportUtils.copyToClipboard(context, "Chat History", transcript)
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Копировать всё")
                        }
                    }
                }

                if (messages.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            title = "Диалог пуст",
                            description = "В этом диалоге еще нет отправленных сообщений.",
                            actionLabel = "Написать первое сообщение",
                            onActionClick = { onOpenChat(chatId) }
                        )
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (msg.isFromUser) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (msg.isFromUser) MaterialTheme.colorScheme.primary else NeonPurple,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    if (msg.isFromUser) Icons.Default.Person else Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = androidx.compose.ui.graphics.Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (msg.isFromUser) "Вы" else "Devs Code AI",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = dateFormat.format(Date(msg.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondaryDark,
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteMessage(msg.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Удалить сообщение",
                                                tint = TextSecondaryDark,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                SelectionContainer {
                                    if (msg.isFromUser) {
                                        Text(
                                            text = msg.content,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    } else {
                                        MarkdownViewer(content = msg.content)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
