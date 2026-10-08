package com.z23u184.studymate.app.logging

import com.z23u184.studymate.domain.logging.LogLevel
import com.z23u184.studymate.domain.logging.LogSink
import com.z23u184.studymate.domain.logging.StudyMateLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLogSink(
    private val logFile: File,
    private val maxFileSizeBytes: Long = DEFAULT_MAX_FILE_SIZE_BYTES,
) : LogSink {
    private val lock = Any()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        logFile.parentFile?.mkdirs()
    }

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val text = buildString {
            append(dateFormat.format(Date()))
            append(' ')
            append(level.name.padEnd(7))
            append(' ')
            append(tag)
            append(" | ")
            append(message)
            if (throwable != null) {
                append('\n')
                append(StudyMateLogger.throwableToString(throwable))
            }
            append('\n')
        }

        synchronized(lock) {
            rotateIfNeeded()
            logFile.appendText(text)
        }
    }

    private fun rotateIfNeeded() {
        if (!logFile.exists() || logFile.length() < maxFileSizeBytes) return

        val oldFile = File(logFile.parentFile, "${logFile.name}.1")
        if (oldFile.exists()) oldFile.delete()
        logFile.renameTo(oldFile)
        logFile.createNewFile()
    }

    companion object {
        private const val DEFAULT_MAX_FILE_SIZE_BYTES = 1_000_000L
    }
}
