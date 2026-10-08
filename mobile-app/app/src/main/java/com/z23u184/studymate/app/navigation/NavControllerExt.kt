package com.z23u184.studymate.app.navigation

import androidx.navigation.NavHostController

fun NavHostController.popBackStackOrNavigate(route: String) {
    val popped = popBackStack()
    if (!popped) {
        navigate(route) {
            popUpTo(graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }
}