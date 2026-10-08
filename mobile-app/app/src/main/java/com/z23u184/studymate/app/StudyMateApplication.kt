package com.z23u184.studymate.app

import android.app.Application
import com.z23u184.studymate.app.di.appModules
import com.z23u184.studymate.app.logging.StudyMateLogging
import com.z23u184.studymate.app.util.UiTextStringProvider
import com.z23u184.studymate.domain.logging.StudyMateLogger
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class StudyMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        StudyMateLogging.configure(this)
        UiTextStringProvider.context = this
        StudyMateLogger.i(TAG, "Application created")
        startKoin {
            androidContext(this@StudyMateApplication)
            modules(appModules())
        }
        StudyMateLogger.i(TAG, "Dependency graph started")
    }

    private companion object {
        const val TAG = "StudyMateApplication"
    }
}
