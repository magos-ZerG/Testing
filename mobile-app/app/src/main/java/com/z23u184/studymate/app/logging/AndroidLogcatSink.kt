package com.z23u184.studymate.app.logging

import android.util.Log
import com.z23u184.studymate.domain.logging.LogLevel
import com.z23u184.studymate.domain.logging.LogSink

class AndroidLogcatSink : LogSink {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
    }
}
