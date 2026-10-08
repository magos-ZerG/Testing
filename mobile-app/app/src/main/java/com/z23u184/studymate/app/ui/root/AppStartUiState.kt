package com.z23u184.studymate.app.ui.root

import com.z23u184.studymate.app.navigation.AppDestinations

data class AppStartUiState(
    val isReady: Boolean = false,
    val startDestination: String = AppDestinations.LOGIN,
)
