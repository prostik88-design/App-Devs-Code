package com.example.ui.screens.about

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onOpenDrawer: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("О приложении Devs Code", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("about_drawer_btn")) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Branding Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(36.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Devs Code", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("AI Software Engineer в вашем смартфоне", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Версия 1.0.0 (Build 2026.08)", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                }
            }

            // Philosophy Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Концепция", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Devs Code — это не просто чат-бот, а полноценная мобильная среда разработки с искусственным интеллектом. " +
                                "Приложение анализирует задачи, проектирует архитектуру, создает и редактирует файлы проектов, проводит аудит безопасности и исправляет ошибки прямо на Android-устройстве.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }

            // Architecture & Privacy
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Архитектура и безопасность", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("✓ Прямое HTTPS подключение к Google Gemini API", style = MaterialTheme.typography.bodySmall)
                    Text("✓ Без промежуточных серверов или сторонних бэкендов", style = MaterialTheme.typography.bodySmall)
                    Text("✓ Все проекты и файлы хранятся локально в Room SQLite", style = MaterialTheme.typography.bodySmall)
                    Text("✓ Полная интеграция с Android Storage Access Framework (SAF)", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Tech Stack & Libraries
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Технологический стек", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Kotlin 2.0+ & Coroutines / Flow", style = MaterialTheme.typography.bodySmall)
                    Text("• Jetpack Compose & Material 3", style = MaterialTheme.typography.bodySmall)
                    Text("• Room Database (KSP) & DataStore Preferences", style = MaterialTheme.typography.bodySmall)
                    Text("• AndroidX Navigation Compose", style = MaterialTheme.typography.bodySmall)
                    Text("• Google Gemini API", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
