package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.CodeReviewDao
import com.example.data.local.dao.ErrorReportDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.ProjectFileDao
import com.example.data.local.dao.ProjectFolderDao
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.CodeReviewEntity
import com.example.data.local.entity.ErrorReportEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectFileEntity
import com.example.data.local.entity.ProjectFolderEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProjectEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        ProjectFileEntity::class,
        ProjectFolderEntity::class,
        CodeReviewEntity::class,
        ErrorReportEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun projectFileDao(): ProjectFileDao
    abstract fun projectFolderDao(): ProjectFolderDao
    abstract fun codeReviewDao(): CodeReviewDao
    abstract fun errorReportDao(): ErrorReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_isArchived` ON `projects` (`isArchived`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_status` ON `projects` (`status`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "devscode_database.db"
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
