package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.CodeReviewEntity
import com.example.data.local.entity.ErrorReportEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectFileEntity
import com.example.data.local.entity.ProjectFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY isArchived ASC, updatedAt DESC")
    fun getAllProjectsIncludingArchived(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE status = :status AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getProjectsByStatus(status: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeProjectById(id: Long): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET isArchived = :isArchived, status = :newStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setProjectArchived(id: Long, isArchived: Boolean, newStatus: String, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getChatsForProject(projectId: Long): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getChatById(id: Long): ChatEntity?

    @Query("SELECT * FROM chats WHERE id = :id")
    fun observeChatById(id: Long): Flow<ChatEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity): Long

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChatById(id: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    suspend fun getMessagesListForChat(chatId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearMessagesForChat(chatId: Long)
}

@Dao
interface ProjectFileDao {
    @Query("SELECT * FROM project_files WHERE projectId = :projectId AND isDeleted = 0 ORDER BY relativePath ASC")
    fun getFilesForProject(projectId: Long): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM project_files WHERE projectId = :projectId AND isDeleted = 0 ORDER BY relativePath ASC")
    suspend fun getFilesListForProject(projectId: Long): List<ProjectFileEntity>

    @Query("SELECT * FROM project_files WHERE id = :id")
    suspend fun getFileById(id: Long): ProjectFileEntity?

    @Query("SELECT * FROM project_files WHERE id = :id")
    fun observeFileById(id: Long): Flow<ProjectFileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<ProjectFileEntity>): List<Long>

    @Update
    suspend fun updateFile(file: ProjectFileEntity)

    @Query("DELETE FROM project_files WHERE id = :id")
    suspend fun deleteFileById(id: Long)
}

@Dao
interface ProjectFolderDao {
    @Query("SELECT * FROM project_folders WHERE projectId = :projectId ORDER BY relativePath ASC")
    fun getFoldersForProject(projectId: Long): Flow<List<ProjectFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: ProjectFolderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<ProjectFolderEntity>): List<Long>

    @Query("DELETE FROM project_folders WHERE id = :id")
    suspend fun deleteFolderById(id: Long)
}

@Dao
interface CodeReviewDao {
    @Query("SELECT * FROM code_reviews ORDER BY createdAt DESC")
    fun getAllReviews(): Flow<List<CodeReviewEntity>>

    @Query("SELECT * FROM code_reviews WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getReviewsForProject(projectId: Long): Flow<List<CodeReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: CodeReviewEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<CodeReviewEntity>): List<Long>

    @Update
    suspend fun updateReview(review: CodeReviewEntity)

    @Query("DELETE FROM code_reviews WHERE id = :id")
    suspend fun deleteReviewById(id: Long)
}

@Dao
interface ErrorReportDao {
    @Query("SELECT * FROM error_reports ORDER BY createdAt DESC")
    fun getAllErrorReports(): Flow<List<ErrorReportEntity>>

    @Query("SELECT * FROM error_reports WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getErrorReportsForProject(projectId: Long): Flow<List<ErrorReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErrorReport(report: ErrorReportEntity): Long

    @Update
    suspend fun updateErrorReport(report: ErrorReportEntity)

    @Query("DELETE FROM error_reports WHERE id = :id")
    suspend fun deleteErrorReportById(id: Long)
}
