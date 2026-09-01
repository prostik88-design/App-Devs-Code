package com.example.ui.screens.projects

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectFileEntity
import com.example.data.repository.DevsCodeRepository
import com.example.util.ProjectExportUtils
import com.example.util.SafFileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProjectFilterTab {
    ALL,
    ACTIVE,
    ARCHIVED
}

data class ProjectsUiState(
    val selectedProject: ProjectEntity? = null,
    val projectFiles: List<ProjectFileEntity> = emptyList(),
    val isLoading: Boolean = false
)

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DevsCodeRepository.getInstance(application)

    private val _selectedTab = MutableStateFlow(ProjectFilterTab.ALL)
    val selectedTab: StateFlow<ProjectFilterTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val rawProjects: StateFlow<List<ProjectEntity>> = repository.getAllProjectsIncludingArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProjects: StateFlow<List<ProjectEntity>> = combine(
        rawProjects,
        _selectedTab,
        _searchQuery
    ) { projects, tab, query ->
        projects.filter { project ->
            val matchesTab = when (tab) {
                ProjectFilterTab.ALL -> true
                ProjectFilterTab.ACTIVE -> !project.isArchived
                ProjectFilterTab.ARCHIVED -> project.isArchived
            }
            val matchesQuery = query.isBlank() ||
                    project.name.contains(query, ignoreCase = true) ||
                    project.description.contains(query, ignoreCase = true) ||
                    project.platform.contains(query, ignoreCase = true) ||
                    project.language.contains(query, ignoreCase = true)
            matchesTab && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectFiles = MutableStateFlow<List<ProjectFileEntity>>(emptyList())
    val selectedProjectFiles: StateFlow<List<ProjectFileEntity>> = _selectedProjectFiles.asStateFlow()

    private val _currentProject = MutableStateFlow<ProjectEntity?>(null)
    val currentProject: StateFlow<ProjectEntity?> = _currentProject.asStateFlow()

    private val _isImportingFiles = MutableStateFlow(false)
    val isImportingFiles: StateFlow<Boolean> = _isImportingFiles.asStateFlow()

    val uiState: StateFlow<ProjectsUiState> = combine(
        _currentProject,
        _selectedProjectFiles,
        _isImportingFiles
    ) { project, files, loading ->
        ProjectsUiState(
            selectedProject = project,
            projectFiles = files,
            isLoading = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProjectsUiState())

    fun setFilterTab(tab: ProjectFilterTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadProject(projectId: Long) {
        viewModelScope.launch {
            _currentProject.value = repository.getProjectById(projectId)
            repository.getFilesForProject(projectId).collect { files ->
                _selectedProjectFiles.value = files
            }
        }
    }

    fun createPresetProject(preset: String) {
        viewModelScope.launch {
            when (preset) {
                "expense_app" -> repository.generateExpenseAppProject()
                "telegram_bot" -> repository.generateTelegramBotProject()
                "website" -> repository.generateWebsiteProject()
                "game" -> repository.generateGameProject()
                "android_app" -> repository.generateAndroidAppProject()
                else -> repository.generateAndroidAppProject()
            }
        }
    }

    fun createNewProject(
        name: String,
        description: String,
        platform: String = "Android",
        language: String = "Kotlin",
        framework: String = "Jetpack Compose",
        architecture: String = "MVVM + Clean Architecture",
        status: String = "ACTIVE"
    ) {
        viewModelScope.launch {
            repository.createProject(
                name = name,
                description = description,
                platform = platform,
                language = language,
                framework = framework,
                architecture = architecture,
                status = status
            )
        }
    }

    fun updateProjectDetails(
        project: ProjectEntity,
        name: String,
        description: String,
        platform: String,
        language: String,
        framework: String,
        architecture: String,
        status: String
    ) {
        viewModelScope.launch {
            repository.updateProject(
                project.copy(
                    name = name,
                    description = description,
                    platform = platform,
                    language = language,
                    framework = framework,
                    architecture = architecture,
                    status = status
                )
            )
            if (_currentProject.value?.id == project.id) {
                _currentProject.value = repository.getProjectById(project.id)
            }
        }
    }

    fun toggleArchiveProject(projectId: Long, currentlyArchived: Boolean) {
        viewModelScope.launch {
            repository.setProjectArchived(projectId, !currentlyArchived)
            if (_currentProject.value?.id == projectId) {
                _currentProject.value = repository.getProjectById(projectId)
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }

    fun createFile(projectId: Long, name: String, relativePath: String, language: String, content: String = "") {
        viewModelScope.launch {
            repository.createProjectFile(
                projectId = projectId,
                name = name,
                relativePath = relativePath,
                language = language,
                content = content
            )
        }
    }

    fun renameFile(fileId: Long, newName: String, newRelativePath: String) {
        viewModelScope.launch {
            repository.renameFile(fileId, newName, newRelativePath)
        }
    }

    fun deleteFile(fileId: Long) {
        viewModelScope.launch {
            repository.deleteFile(fileId)
        }
    }

    /**
     * Imports a single file chosen via SAF ACTION_OPEN_DOCUMENT.
     */
    fun importSingleSafFile(context: Context, projectId: Long, uri: Uri) {
        viewModelScope.launch {
            _isImportingFiles.value = true
            try {
                SafFileManager.takePersistablePermissions(context, uri)
                val readResult = SafFileManager.readSingleFile(context, uri)
                readResult.onSuccess { imported ->
                    repository.importSafFile(
                        projectId = projectId,
                        name = imported.name,
                        relativePath = imported.relativePath,
                        language = imported.language,
                        content = imported.content,
                        size = imported.size
                    )
                    Toast.makeText(context, "Импортирован файл «${imported.name}» (${imported.formattedSize})", Toast.LENGTH_SHORT).show()
                }.onFailure { e ->
                    Toast.makeText(context, "Ошибка импорта: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Сбой SAF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isImportingFiles.value = false
            }
        }
    }

    /**
     * Imports an entire folder tree chosen via SAF ACTION_OPEN_DOCUMENT_TREE.
     */
    fun importFolderSafTree(context: Context, projectId: Long, treeUri: Uri) {
        viewModelScope.launch {
            _isImportingFiles.value = true
            try {
                SafFileManager.takePersistablePermissions(context, treeUri)
                val result = SafFileManager.importDirectoryTree(context, treeUri)
                result.onSuccess { files ->
                    for (file in files) {
                        repository.importSafFile(
                            projectId = projectId,
                            name = file.name,
                            relativePath = file.relativePath,
                            language = file.language,
                            content = file.content,
                            size = file.size
                        )
                    }
                    val totalBytes = files.sumOf { it.size }
                    val formatted = SafFileManager.formatFileSize(totalBytes)
                    Toast.makeText(context, "Импортировано файлов: ${files.size} ($formatted)", Toast.LENGTH_LONG).show()
                }.onFailure { e ->
                    Toast.makeText(context, "Ошибка импорта папки: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Сбой импорта дерева файлов: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                _isImportingFiles.value = false
            }
        }
    }

    fun exportProjectToZipUri(context: Context, projectId: Long, uri: Uri) {
        viewModelScope.launch {
            try {
                val project = repository.getProjectById(projectId) ?: return@launch
                val files = repository.getFilesListForProject(projectId)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    ProjectExportUtils.writeProjectZip(project, files, outputStream)
                }
                Toast.makeText(context, "ZIP-архив успешно сохранен на диск!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка сохранения ZIP: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun shareProjectZip(context: Context, projectId: Long) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            val files = repository.getFilesListForProject(projectId)
            ProjectExportUtils.shareProjectAsZip(context, project, files)
        }
    }

    fun copyAllProjectCode(context: Context, projectId: Long) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            val files = repository.getFilesListForProject(projectId)
            val markdown = ProjectExportUtils.generateProjectMarkdown(project, files)
            ProjectExportUtils.copyToClipboard(context, "DevsCode_${project.name}", markdown)
        }
    }

    fun shareProjectMarkdown(context: Context, projectId: Long) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            val files = repository.getFilesListForProject(projectId)
            val markdown = ProjectExportUtils.generateProjectMarkdown(project, files)
            ProjectExportUtils.shareText(context, "Проект: ${project.name}", markdown)
        }
    }
}
