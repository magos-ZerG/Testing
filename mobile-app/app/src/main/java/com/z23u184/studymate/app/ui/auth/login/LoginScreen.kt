package com.z23u184.studymate.app.ui.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.z23u184.studymate.app.navigation.AppDestinations
import com.z23u184.studymate.app.ui.auth.login.component.*
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.ui.common.component.LoadingContent
import com.z23u184.studymate.app.ui.common.component.MainInfoCard
import com.z23u184.studymate.app.ui.common.component.StudyMateScaffold
import org.koin.androidx.compose.koinViewModel

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun LoginScreen(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    viewModel: LoginScreenViewModel = koinViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val navigateBackToTopics = {
        val popped = navController.popBackStack(AppDestinations.TOPICS, inclusive = false)
        if (!popped) {
            navController.navigate(AppDestinations.TOPICS) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

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
        StudyMateScaffold(
            snackbarHostState = snackbarHostState,
            title = stringResource(R.string.login_title),
            onBackClick = navigateBackToTopics,
        ) {
            LoadingContent(message = stringResource(R.string.login_progress))
        }
        return
    }

    StudyMateScaffold(
        snackbarHostState = snackbarHostState,
        title = stringResource(R.string.study_mate),
        onBackClick = navigateBackToTopics,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            MainInfoCard {
                Text(
                    text = stringResource(R.string.login_account_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                LoginForm(state, viewModel::onEmailChanged, viewModel::onPasswordChanged)
                LoginActions(state, viewModel::onLoginClick, viewModel::onRegisterClick)
            }
        }
    }
}
