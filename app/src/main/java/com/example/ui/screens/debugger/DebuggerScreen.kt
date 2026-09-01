package com.example.ui.screens.debugger

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MarkdownViewer
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebuggerScreen(
    onOpenDrawer: () -> Unit = {},
    viewModel: DebuggerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val quickErrors = listOf(
        "Fatal Exception: NullPointerException" to """
java.lang.NullPointerException: Attempt to invoke virtual method 'void androidx.lifecycle.LiveData.observe(androidx.lifecycle.LifecycleOwner, androidx.lifecycle.Observer)' on a null object reference
    at com.devscode.app.MainActivity.onCreate(MainActivity.kt:42)
    at android.app.Activity.performCreate(Activity.java:8000)
""".trimIndent(),
        "Room Schema Migration Error" to """
java.lang.IllegalStateException: Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number. You can simply fix this by increasing the version number. Expected: TableInfo{...}
    at androidx.room.RoomOpenHelper.checkIdentity(RoomOpenHelper.kt:125)
""".trimIndent(),
        "Gradle KSP Compatibility" to """
e: [ksp] Cannot find symbol: com.devscode.app.data.local.dao.TaskDao_Impl.
Compilation failed with exit code 1 in task ':app:kspDebugKotlin'.
""".trimIndent(),
        "Android White Screen on Launch" to """
Android Runtime Warning: Activity com.devscode.app/.MainActivity has leaked window DecorView@a1b2c3d that was originally added here.
Activity did not complete draw within 5000ms.
""".trimIndent()
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Debugger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("debugger_drawer_btn")) {
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
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🐞 Интеллектуальный анализатор ошибок и сбоев",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Вставьте Stack Trace, лог из Logcat или ошибку сборки Gradle. AI определит причину, укажет файл со строкой и выдаст готовый исправленный код.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }
            }

            // Quick Samples
            Text(
                text = "Типовые сценарии ошибок:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickErrors.forEach { (title, errorText) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            viewModel.setErrorInput(errorText)
                            viewModel.analyzeError(errorText)
                        },
                        label = { Text(title, fontSize = 12.sp) }
                    )
                }
            }

            // Error Input Field
            OutlinedTextField(
                value = uiState.errorInput,
                onValueChange = { viewModel.setErrorInput(it) },
                label = { Text("Вставьте Stack Trace, Logcat или ошибку компиляции") },
                placeholder = { Text("Вставьте текст ошибки...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("debugger_error_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            // Analyze Button
            Button(
                onClick = { viewModel.analyzeError() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("run_debugger_btn"),
                enabled = uiState.errorInput.isNotBlank() && !uiState.isAnalyzing,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (uiState.isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI анализирует стек-трейс...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Диагностировать ошибку", fontWeight = FontWeight.Bold)
                }
            }

            // Results View
            if (!uiState.analysisResult.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🔬 Результат диагностики AI Debugger:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MarkdownViewer(content = uiState.analysisResult!!)
                    }
                }
            }
        }
    }
}
