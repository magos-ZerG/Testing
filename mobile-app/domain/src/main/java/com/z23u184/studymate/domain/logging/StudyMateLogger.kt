package com.z23u184.studymate.domain.logging

import java.io.PrintWriter
import java.io.StringWriter

enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

interface LogSink {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
}

object StudyMateLogger {
    @Volatile
    private var minLevel: LogLevel = LogLevel.INFO

    @Volatile
    private var sinks: List<LogSink> = emptyList()

    fun configure(minLevel: LogLevel, sinks: List<LogSink>) {
        this.minLevel = minLevel
        this.sinks = sinks
        i(TAG, "Logger configured: minLevel=$minLevel sinks=${sinks.size}")
    }

    fun v(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.VERBOSE, tag, message, throwable)
    fun d(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.DEBUG, tag, message, throwable)
    fun i(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.INFO, tag, message, throwable)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)

    fun throwableToString(throwable: Throwable): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (level.ordinal < minLevel.ordinal) return

        val currentSinks = sinks
        if (currentSinks.isEmpty()) return

        currentSinks.forEach { sink ->
            runCatching { sink.log(level, tag, message, throwable) }
        }
    }

    private const val TAG = "StudyMateLogger"
}
