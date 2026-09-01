package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [Index("isArchived"), Index("status")]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val platform: String = "Android",
    val language: String = "Kotlin",
    val framework: String = "Jetpack Compose",
    val architecture: String = "MVVM + Clean Architecture",
    val status: String = "ACTIVE", // "ACTIVE", "IN_PROGRESS", "COMPLETED", "ARCHIVED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val settingsJson: String = "{}"
)

@Entity(
    tableName = "chats",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("projectId")]
)
data class ChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long? = null,
    val title: String = "New Chat",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false
)

typealias ChatSessionEntity = ChatEntity

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatId")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chatId: Long,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modelName: String = "gemini-3.5-flash",
    val isError: Boolean = false,
    val isStreaming: Boolean = false,
    val tokenUsage: Int = 0,
    val attachedFileIds: String = "" // Comma-separated or JSON list of file IDs
) {
    val isFromUser: Boolean get() = role.equals("user", ignoreCase = true)
    val timestamp: Long get() = createdAt
}

@Entity(
    tableName = "project_files",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("parentFolderId")]
)
data class ProjectFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val parentFolderId: Long? = null,
    val name: String,
    val relativePath: String,
    val language: String = "kotlin",
    val content: String = "",
    val mimeType: String = "text/plain",
    val fileSize: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "project_folders",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("parentFolderId")]
)
data class ProjectFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val parentFolderId: Long? = null,
    val name: String,
    val relativePath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "code_reviews",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("fileId"), Index("chatId")]
)
data class CodeReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long? = null,
    val fileId: Long? = null,
    val chatId: Long? = null,
    val severity: String, // "CRITICAL", "WARNING", "IMPROVEMENT", "GOOD"
    val title: String,
    val description: String,
    val suggestion: String,
    val fixedCode: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false
)

@Entity(
    tableName = "error_reports",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId"), Index("chatId")]
)
data class ErrorReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long? = null,
    val chatId: Long? = null,
    val errorType: String,
    val errorText: String,
    val analysis: String,
    val solution: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false
)
