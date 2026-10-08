package com.z23u184.studymate.app.ui.settings

import android.content.Context
import android.os.LocaleList
import java.util.Locale

fun Context.applyAppLocale(language: AppLanguage) {
    val locale = Locale.forLanguageTag(language.tag)

    Locale.setDefault(locale)

    val configuration = resources.configuration
    configuration.setLocale(locale)
    configuration.setLocales(LocaleList(locale))
    configuration.setLayoutDirection(locale)

    @Suppress("DEPRECATION")
    resources.updateConfiguration(configuration, resources.displayMetrics)
}