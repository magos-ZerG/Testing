package com.z23u184.studymate.app.ui.topic.topiclist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.lifecycle.Lifecycle
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.ui.common.component.ConfirmDialog
import com.z23u184.studymate.app.ui.common.component.LoadingContent
import com.z23u184.studymate.app.ui.common.component.StudyMateScaffold
import org.koin.androidx.compose.koinViewModel
import com.z23u184.studymate.app.ui.topic.topiclist.componentTest.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun TopicListTestScreen(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    viewModel: TopicListScreenViewModel = koinViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    var showProfileDialog by remember { mutableStateOf(false) }
    var topicIdPendingDelete by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.NavigateTo -> navController.navigate(event.route) { launchSingleTop = true }
                UiEvent.NavigateBack -> navController.popBackStack()
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message.asString())
                is UiEvent.OpenAttachment -> Unit
            }
        }
    }

    topicIdPendingDelete?.let { topicId ->
        ConfirmDialog(
            title = stringResource(R.string.delete_topic_question),
            text = stringResource(R.string.delete_topic_text),
            onConfirm = {
                topicIdPendingDelete = null
                viewModel.onConfirmDeleteTopic(topicId)
            },
            onDismiss = { topicIdPendingDelete = null },
        )
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text(stringResource(R.string.profile)) },
            text = {
                Text(
                    state.userDisplayName
                        ?: if (state.isAuthorized) stringResource(R.string.authorized_mode) else stringResource(R.string.local_mode)
                )
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            },
            dismissButton = {
                if (state.isAuthorized) {
                    TextButton(onClick = {
                        showProfileDialog = false
                        viewModel.onLogoutClick()
                    }) {
                        Text(stringResource(R.string.logout_from_account))
                    }
                } else {
                    TextButton(onClick = {
                        showProfileDialog = false
                        viewModel.onLoginClick()
                    }) {
                        Text(stringResource(R.string.login))
                    }
                }
            },
        )
    }

    if (state.isLogoutInProgress) {
        StudyMateScaffold(
            snackbarHostState = snackbarHostState,
            title = stringResource(R.string.logout_title),
        ) {
            LoadingContent(message = stringResource(R.string.logout_progress))
        }
        return
    }

    StudyMateScaffold(
        snackbarHostState = snackbarHostState,
        title = stringResource(R.string.topics),
        actions = {
            TopicListTopBar(
                isAuthorized = state.isAuthorized,
                onProfileClick = { showProfileDialog = true },
                onLogoutClick = viewModel::onLogoutClick,
                onLoginClick = viewModel::onLoginClick,
            )
        },
        floatingActionIcon = Icons.Default.Add,
        floatingActionContentDescription = stringResource(R.string.create_topic),
        onFloatingActionClick = viewModel::onCreateTopicClick,
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            TopicListContent(
                state = state,
                onTopicClick = viewModel::onTopicClick,
                onDeleteTopicClick = { topicIdPendingDelete = it },
                onRetry = viewModel::load,
            )
        }
    }
}
