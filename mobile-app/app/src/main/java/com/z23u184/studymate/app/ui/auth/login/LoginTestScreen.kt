package com.z23u184.studymate.app.ui.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.z23u184.studymate.app.navigation.AppDestinations
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.ui.common.component.LoadingContent
import com.z23u184.studymate.app.ui.common.component.StudyMateScaffold
import org.koin.androidx.compose.koinViewModel
import com.z23u184.studymate.app.ui.auth.login.componentTest.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun LoginTestScreen(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    viewModel: LoginScreenViewModel = koinViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    if (event.route == AppDestinations.TOPICS) {
                        navController.navigate(event.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(event.route) { launchSingleTop = true }
                    }
                }
                UiEvent.NavigateBack -> navController.popBackStack()
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message.asString())
                is UiEvent.OpenAttachment -> Unit
            }
        }
    }

    if (state.isLoading) {
        StudyMateScaffold(snackbarHostState = snackbarHostState, title = stringResource(R.string.login_title)) {
            LoadingContent(message = stringResource(R.string.login_progress))
        }
        return
    }

    StudyMateScaffold(snackbarHostState = snackbarHostState, title = stringResource(R.string.login_title)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.login_prompt))
            LoginForm(state, viewModel::onEmailChanged, viewModel::onPasswordChanged)
            LoginActions(state, viewModel::onLoginClick, viewModel::onRegisterClick)
        }
    }
}
