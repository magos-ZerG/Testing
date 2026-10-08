package com.z23u184.studymate.app.logging

import android.content.Context
import com.z23u184.studymate.app.BuildConfig
import com.z23u184.studymate.domain.logging.LogLevel
import com.z23u184.studymate.domain.logging.StudyMateLogger
import java.io.File

object StudyMateLogging {
    const val LOG_FILE_NAME = "studymate.log"

    fun configure(context: Context) {
        val file = logFile(context)
        val minLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO
        StudyMateLogger.configure(
            minLevel = minLevel,
            sinks = listOf(
                AndroidLogcatSink(),
                FileLogSink(file),
            )
        )
        StudyMateLogger.i(TAG, "Log file: ${file.absolutePath}")
    }

    fun logFile(context: Context): File = File(File(context.filesDir, "logs"), LOG_FILE_NAME)

    private const val TAG = "StudyMateLogging"
}
