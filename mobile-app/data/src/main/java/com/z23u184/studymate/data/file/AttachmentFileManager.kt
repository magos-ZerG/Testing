package com.z23u184.studymate.data.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream

class AttachmentFileManager(
    private val context: Context,
) {
    fun ensureAppPrivateCopy(sourceUri: Uri, preferredFileName: String): File {
        val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
        val safeName = preferredFileName.ifBlank { "attachment_${System.currentTimeMillis()}" }
        val targetFile = uniqueFile(attachmentsDir, safeName)
        context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Cannot open input stream for $sourceUri" }
            FileOutputStream(targetFile).use { output -> input.copyTo(output) }
        }
        return targetFile
    }

    fun saveDownloadedBytes(fileName: String, bytes: ByteArray): File {
        val downloadsDir = File(context.cacheDir, "attachment_downloads").apply { mkdirs() }
        val targetFile = uniqueFile(downloadsDir, fileName.ifBlank { "attachment_${System.currentTimeMillis()}" })
        FileOutputStream(targetFile).use { it.write(bytes) }
        return targetFile
    }

    fun queryDisplayName(uri: Uri): String? = queryOpenableColumn(uri, OpenableColumns.DISPLAY_NAME)

    fun querySizeBytes(uri: Uri): Long? = queryOpenableColumn(uri, OpenableColumns.SIZE)?.toLongOrNull()

    fun resolveMimeType(uri: Uri): String? = context.contentResolver.getType(uri)

    fun resolveMimeType(file: File): String? {
        val extension = file.extension.takeIf { it.isNotBlank() }?.lowercase()
        return extension?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
    }

    fun getFileOrNull(localPath: String?): File? {
        if (localPath.isNullOrBlank()) return null
        val file = File(localPath)
        return file.takeIf(File::exists)
    }

    fun readBytes(file: File): ByteArray = file.readBytes()

    fun deleteIfExists(localPath: String?) {
        getFileOrNull(localPath)?.delete()
    }

    private fun queryOpenableColumn(uri: Uri, columnName: String): String? {
        return context.contentResolver.query(uri, arrayOf(columnName), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(columnName)
            if (index == -1 || !cursor.moveToFirst()) return@use null
            cursor.getString(index)
        }
    }

    private fun uniqueFile(directory: File, fileName: String): File {
        val base = File(fileName).nameWithoutExtension.ifBlank { "attachment" }
        val ext = File(fileName).extension
        var candidate = File(directory, fileName)
        var index = 1
        while (candidate.exists()) {
            val suffix = "_$index"
            candidate = File(directory, if (ext.isBlank()) "$base$suffix" else "$base$suffix.$ext")
            index++
        }
        return candidate
    }
}
