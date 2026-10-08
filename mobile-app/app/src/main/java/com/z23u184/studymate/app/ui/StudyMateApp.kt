package com.z23u184.studymate.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.z23u184.studymate.app.navigation.AppNavGraph
import com.z23u184.studymate.app.ui.common.component.LoadingContent
import com.z23u184.studymate.app.ui.root.AppStartViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun StudyMateApp(
    viewModel: AppStartViewModel = koinViewModel(),
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    if (!state.isReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingContent()
        }
        return
    }

    AppNavGraph(
        navController = navController,
        snackbarHostState = snackbarHostState,
        startDestination = state.startDestination,
    )
}
