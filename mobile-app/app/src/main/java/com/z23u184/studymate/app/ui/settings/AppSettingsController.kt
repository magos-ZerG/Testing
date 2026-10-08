package com.z23u184.studymate.app.ui.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsController(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppUiSettings> = _settings.asStateFlow()

    fun setDarkTheme(isDarkTheme: Boolean) {
        update(_settings.value.copy(isDarkTheme = isDarkTheme))
    }

    fun setLanguage(language: AppLanguage) {
        update(_settings.value.copy(language = language))
    }

    private fun update(settings: AppUiSettings) {
        preferences.edit()
            .putBoolean(KEY_DARK_THEME, settings.isDarkTheme)
            .putString(KEY_LANGUAGE, settings.language.name)
            .apply()
        _settings.value = settings
    }

    private fun loadSettings(): AppUiSettings = AppUiSettings(
        isDarkTheme = preferences.getBoolean(KEY_DARK_THEME, false),
        language = preferences.getString(KEY_LANGUAGE, AppLanguage.RU.name)
            ?.let { stored -> AppLanguage.entries.firstOrNull { it.name == stored } }
            ?: AppLanguage.RU,
    )

    private companion object {
        const val PREFERENCES_NAME = "ui_settings"
        const val KEY_DARK_THEME = "dark_theme"
        const val KEY_LANGUAGE = "language"
    }
}
