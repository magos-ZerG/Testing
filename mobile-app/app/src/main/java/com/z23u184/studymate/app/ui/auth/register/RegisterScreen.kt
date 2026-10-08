package com.z23u184.studymate.app.ui.auth.register

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
import androidx.navigation.NavHostController
import com.z23u184.studymate.app.navigation.AppDestinations
import com.z23u184.studymate.app.navigation.popBackStackOrNavigate
import com.z23u184.studymate.app.ui.auth.register.component.*
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.ui.common.component.MainInfoCard
import com.z23u184.studymate.app.ui.common.component.StudyMateScaffold
import org.koin.androidx.compose.koinViewModel

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun RegisterScreen(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    viewModel: RegisterScreenViewModel = koinViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    if (event.route == AppDestinations.LOGIN) {
                        navController.navigate(event.route) {
                            popUpTo(AppDestinations.LOGIN) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate(event.route) { launchSingleTop = true }
                    }
                }
                UiEvent.NavigateBack -> navController.popBackStackOrNavigate(AppDestinations.LOGIN)
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message.asString())
                is UiEvent.OpenAttachment -> Unit
            }
        }
    }

    StudyMateScaffold(
        snackbarHostState = snackbarHostState,
        title = stringResource(R.string.study_mate),
        onBackClick = viewModel::onBackToLogin,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            MainInfoCard {
                Text(stringResource(R.string.register_title), style = MaterialTheme.typography.headlineSmall)
                RegisterForm(state, viewModel::onEmailChanged, viewModel::onPasswordChanged, viewModel::onConfirmPasswordChanged)
                RegisterActions(state, viewModel::onRegisterClick, viewModel::onBackToLogin)
            }
        }
    }
}
