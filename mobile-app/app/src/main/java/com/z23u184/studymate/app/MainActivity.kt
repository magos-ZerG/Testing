package com.z23u184.studymate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.z23u184.studymate.app.ui.StudyMateApp
import com.z23u184.studymate.app.ui.settings.AppSettingsController
import com.z23u184.studymate.app.ui.settings.LocalAppSettingsController
import com.z23u184.studymate.app.ui.settings.applyAppLocale
import com.z23u184.studymate.app.ui.theme.StudyMateTheme
import com.z23u184.studymate.app.util.UiTextStringProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val settingsController = AppSettingsController(applicationContext)

        applyAppLocale(settingsController.settings.value.language)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by settingsController.settings.collectAsStateWithLifecycle()

            SideEffect {
                this@MainActivity.applyAppLocale(settings.language)
                UiTextStringProvider.context = this@MainActivity
            }

            key(settings.language) {
                CompositionLocalProvider(
                    LocalActivityResultRegistryOwner provides this@MainActivity,
                    LocalAppSettingsController provides settingsController,
                ) {
                    StudyMateTheme(darkTheme = settings.isDarkTheme) {
                        StudyMateApp()
                    }
                }
            }
        }
    }
}
