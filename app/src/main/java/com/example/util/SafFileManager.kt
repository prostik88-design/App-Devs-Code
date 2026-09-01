package com.example.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Metadata and payload of a file imported via Android Storage Access Framework (SAF).
 */
data class ImportedFile(
    val name: String,
    val extension: String,
    val size: Long,
    val formattedSize: String,
    val lastModified: Long,
    val formattedDate: String,
    val language: String,
    val relativePath: String,
    val content: String,
    val uriString: String,
    val mimeType: String
)

/**
 * Validation summary for a selected file.
 */
data class FileValidation(
    val isValid: Boolean,
    val name: String = "",
    val size: Long = 0L,
    val errorMessage: String? = null
)

object SafFileManager {

    private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB limit for text/code files
    private const val WARN_FILE_SIZE_BYTES = 2 * 1024 * 1024L // 2 MB warning threshold

    private val SUPPORTED_CODE_EXTENSIONS = setOf(
        "kt", "kts", "java", "xml", "json", "gradle", "properties", "toml", "env",
        "py", "js", "ts", "tsx", "jsx", "html", "css", "scss", "sass", "less",
        "c", "cpp", "h", "hpp", "cs", "go", "rs", "swift", "php", "rb", "sql",
        "sh", "bash", "zsh", "bat", "ps1", "md", "txt", "yaml", "yml", "ini",
        "conf", "svg", "proto", "graphql", "dockerfile", "gitignore", "pro"
    )

    /**
     * Persists URI read/write permissions for a picked document or document tree.
     */
    fun takePersistablePermissions(context: Context, uri: Uri, isWriteNeeded: Boolean = false) {
        try {
            var flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (isWriteNeeded) {
                flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            }
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: SecurityException) {
            // Some URI providers may not support persistable flags; non-fatal
        } catch (e: Exception) {
            // Fallback
        }
    }

    /**
     * Checks if a file exists and is accessible via the given URI.
     */
    suspend fun checkUriAccessibility(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Inspects file metadata from ContentResolver without loading entire file into memory.
     */
    suspend fun inspectFileMetadata(context: Context, uri: Uri): FileValidation = withContext(Dispatchers.IO) {
        var displayName = "unknown_file"
        var size: Long = -1L

        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                    if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }

            if (size > MAX_FILE_SIZE_BYTES) {
                return@withContext FileValidation(
                    isValid = false,
                    name = displayName,
                    size = size,
                    errorMessage = "Размер файла превышает 10 МБ (${formatFileSize(size)}). Выберите файл меньшего размера."
                )
            }

            FileValidation(
                isValid = true,
                name = displayName,
                size = size
            )
        } catch (e: Exception) {
            FileValidation(
                isValid = false,
                name = displayName,
                size = size,
                errorMessage = "Не удалось прочитать метаданные файла: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Reads a single file safely from SAF Uri on Dispatchers.IO.
     * Performs validation of existence, accessibility, size, and encoding.
     */
    suspend fun readSingleFile(
        context: Context,
        uri: Uri,
        customRelativePath: String? = null
    ): Result<ImportedFile> = withContext(Dispatchers.IO) {
        try {
            var fileName = "file.txt"
            var fileSize: Long = 0L

            // 1. Query metadata
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) fileName = cursor.getString(nameIdx) ?: fileName
                    if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) fileSize = cursor.getLong(sizeIdx)
                }
            }

            // 2. Validate Size
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                return@withContext Result.failure(
                    IllegalArgumentException("Файл «$fileName» слишком большой (${formatFileSize(fileSize)}). Лимит: 10 МБ.")
                )
            }

            // 3. Read content safely with character encoding detection
            val contentBuilder = StringBuilder()
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Не удалось открыть поток для чтения URI."))

            var isLikelyBinary = false
            inputStream.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    val buffer = CharArray(4096)
                    var charsRead: Int
                    var totalRead = 0

                    while (reader.read(buffer).also { charsRead = it } != -1) {
                        // Check for null characters indicative of binary data (e.g. .class, .dex, .so, .png)
                        for (i in 0 until charsRead) {
                            if (buffer[i] == '\u0000') {
                                isLikelyBinary = true
                                break
                            }
                        }
                        if (isLikelyBinary) break

                        contentBuilder.append(buffer, 0, charsRead)
                        totalRead += charsRead

                        if (totalRead > MAX_FILE_SIZE_BYTES) {
                            return@withContext Result.failure(
                                IllegalStateException("Превышен лимит размера при чтении содержимого файла.")
                            )
                        }
                    }
                }
            }

            if (isLikelyBinary) {
                return@withContext Result.failure(
                    IllegalArgumentException("Файл «$fileName» является бинарным или поврежденным. Поддерживаются только текстовые файлы и исходный код.")
                )
            }

            val content = contentBuilder.toString()
            val ext = fileName.substringAfterLast(".", "").lowercase()
            val lang = detectLanguage(fileName)
            val relPath = customRelativePath ?: fileName
            val lastModified = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

            val imported = ImportedFile(
                name = fileName,
                extension = ext,
                size = if (fileSize > 0) fileSize else content.toByteArray(Charsets.UTF_8).size.toLong(),
                formattedSize = formatFileSize(if (fileSize > 0) fileSize else content.length.toLong()),
                lastModified = lastModified,
                formattedDate = dateFormat.format(Date(lastModified)),
                language = lang,
                relativePath = relPath,
                content = content,
                uriString = uri.toString(),
                mimeType = context.contentResolver.getType(uri) ?: "text/plain"
            )

            Result.success(imported)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recursively traverses a folder tree selected via ACTION_OPEN_DOCUMENT_TREE
     * and extracts all code/text source files.
     */
    suspend fun importDirectoryTree(
        context: Context,
        treeUri: Uri,
        maxFiles: Int = 150
    ): Result<List<ImportedFile>> = withContext(Dispatchers.IO) {
        try {
            takePersistablePermissions(context, treeUri, isWriteNeeded = false)

            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

            val importedFiles = mutableListOf<ImportedFile>()
            val visitedPaths = mutableSetOf<String>()

            traverseDocumentDirectory(
                context = context,
                treeUri = treeUri,
                parentDocId = documentId,
                currentRelativeDir = "",
                importedFiles = importedFiles,
                visitedPaths = visitedPaths,
                maxFiles = maxFiles
            )

            if (importedFiles.isEmpty()) {
                Result.failure(NoSuchElementException("В выбранной папке не найдено подходящих файлов с исходным кодом или текстом."))
            } else {
                Result.success(importedFiles)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun traverseDocumentDirectory(
        context: Context,
        treeUri: Uri,
        parentDocId: String,
        currentRelativeDir: String,
        importedFiles: MutableList<ImportedFile>,
        visitedPaths: MutableSet<String>,
        maxFiles: Int
    ) {
        if (importedFiles.size >= maxFiles) return

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(childrenUri, projection, null, null, null)
            if (cursor == null) return

            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modifiedIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext() && importedFiles.size < maxFiles) {
                val docId = cursor.getString(idIdx)
                val displayName = cursor.getString(nameIdx) ?: continue
                val mimeType = cursor.getString(mimeIdx) ?: ""
                val size = if (!cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                val lastModified = if (!cursor.isNull(modifiedIdx)) cursor.getLong(modifiedIdx) else System.currentTimeMillis()

                // Skip hidden folders and common build/generated directories
                if (displayName.startsWith(".") ||
                    displayName == "node_modules" ||
                    displayName == "build" ||
                    displayName == ".gradle" ||
                    displayName == ".git" ||
                    displayName == "dist" ||
                    displayName == "target" ||
                    displayName == ".idea"
                ) {
                    continue
                }

                val isDirectory = mimeType == DocumentsContract.Document.MIME_TYPE_DIR

                if (isDirectory) {
                    val nextDir = if (currentRelativeDir.isEmpty()) displayName else "$currentRelativeDir/$displayName"
                    traverseDocumentDirectory(
                        context = context,
                        treeUri = treeUri,
                        parentDocId = docId,
                        currentRelativeDir = nextDir,
                        importedFiles = importedFiles,
                        visitedPaths = visitedPaths,
                        maxFiles = maxFiles
                    )
                } else {
                    // Check if supported code/text file
                    val ext = displayName.substringAfterLast(".", "").lowercase()
                    if (isSupportedExtension(ext) && size <= MAX_FILE_SIZE_BYTES) {
                        val fileRelativePath = if (currentRelativeDir.isEmpty()) displayName else "$currentRelativeDir/$displayName"
                        if (visitedPaths.contains(fileRelativePath)) continue
                        visitedPaths.add(fileRelativePath)

                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        try {
                            val content = readTextFromUri(context, docUri)
                            if (content != null) {
                                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                importedFiles.add(
                                    ImportedFile(
                                        name = displayName,
                                        extension = ext,
                                        size = if (size > 0) size else content.toByteArray(Charsets.UTF_8).size.toLong(),
                                        formattedSize = formatFileSize(if (size > 0) size else content.length.toLong()),
                                        lastModified = if (lastModified > 0) lastModified else System.currentTimeMillis(),
                                        formattedDate = dateFormat.format(Date(if (lastModified > 0) lastModified else System.currentTimeMillis())),
                                        language = detectLanguage(displayName),
                                        relativePath = fileRelativePath,
                                        content = content,
                                        uriString = docUri.toString(),
                                        mimeType = mimeType
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // Skip non-readable individual file without failing whole import
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore directory traversal error for single subfolder
        } finally {
            cursor?.close()
        }
    }

    private fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    val builder = StringBuilder()
                    val buffer = CharArray(4096)
                    var read: Int
                    while (reader.read(buffer).also { read = it } != -1) {
                        for (i in 0 until read) {
                            if (buffer[i] == '\u0000') return null // Binary file detected
                        }
                        builder.append(buffer, 0, read)
                    }
                    builder.toString()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Writes content to an external SAF Uri (ACTION_CREATE_DOCUMENT).
     */
    suspend fun writeContentToUri(context: Context, uri: Uri, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: return@withContext Result.failure(IllegalStateException("Не удалось открыть поток записи для указанного URI."))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if a file extension is recognized as source code or text configuration.
     */
    fun isSupportedExtension(extension: String): Boolean {
        return SUPPORTED_CODE_EXTENSIONS.contains(extension.lowercase().trimStart('.'))
    }

    /**
     * Detects programming language based on file extension and name.
     */
    fun detectLanguage(fileName: String): String {
        val lower = fileName.lowercase()
        val ext = lower.substringAfterLast(".", "")

        return when {
            lower == "dockerfile" -> "dockerfile"
            lower == "gemfile" -> "ruby"
            lower == "makefile" -> "makefile"
            ext in listOf("kt", "kts") -> "kotlin"
            ext == "java" -> "java"
            ext in listOf("py", "pyw") -> "python"
            ext in listOf("js", "mjs", "cjs") -> "javascript"
            ext in listOf("ts", "mts") -> "typescript"
            ext == "tsx" -> "tsx"
            ext == "jsx" -> "jsx"
            ext in listOf("cpp", "cc", "cxx") -> "cpp"
            ext == "c" -> "c"
            ext in listOf("h", "hpp") -> "c"
            ext == "cs" -> "csharp"
            ext == "go" -> "go"
            ext == "rs" -> "rust"
            ext == "swift" -> "swift"
            ext == "php" -> "php"
            ext == "rb" -> "ruby"
            ext == "sql" -> "sql"
            ext in listOf("sh", "bash", "zsh") -> "shell"
            ext in listOf("html", "htm") -> "html"
            ext in listOf("css", "scss", "sass", "less") -> "css"
            ext == "xml" -> "xml"
            ext == "json" -> "json"
            ext in listOf("yaml", "yml") -> "yaml"
            ext == "md" -> "markdown"
            ext == "gradle" -> "groovy"
            ext in listOf("properties", "env", "ini", "conf", "toml") -> "properties"
            else -> "text"
        }
    }

    /**
     * Formats bytes into human readable string (e.g. "350 B", "14.2 KB", "1.8 MB").
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
