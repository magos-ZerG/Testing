package com.z23u184.studymate.app.ui.task.tasklist

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.z23u184.studymate.app.R
import com.z23u184.studymate.app.navigation.AppDestinations
import com.z23u184.studymate.app.navigation.popBackStackOrNavigate
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.ui.common.component.ConfirmDialog
import com.z23u184.studymate.app.ui.common.component.StudyMateScaffold
import org.koin.androidx.compose.koinViewModel
import com.z23u184.studymate.app.ui.task.tasklist.component.*

@Composable
fun TopicTaskListScreen(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    viewModel: TopicTaskListScreenViewModel = koinViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    var taskIdPendingDelete by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.NavigateTo -> navController.navigate(event.route) { launchSingleTop = true }
                UiEvent.NavigateBack -> navController.popBackStackOrNavigate(AppDestinations.TOPICS)
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message.asString())
                is UiEvent.OpenAttachment -> Unit
            }
        }
    }

    taskIdPendingDelete?.let { taskId ->
        ConfirmDialog(
            title = stringResource(R.string.delete_task_question),
            text = stringResource(R.string.delete_task_text),
            onConfirm = {
                taskIdPendingDelete = null
                viewModel.onConfirmDeleteTask(taskId)
            },
            onDismiss = { taskIdPendingDelete = null },
        )
    }

    StudyMateScaffold(
        snackbarHostState = snackbarHostState,
        title = state.topicTitle.ifBlank { stringResource(R.string.questions) },
        onBackClick = viewModel::onBackClick,
        floatingActionIcon = Icons.Default.Add,
        floatingActionContentDescription = stringResource(R.string.create_task),
        onFloatingActionClick = viewModel::onCreateTaskClick,
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.padding(padding)) {
            TopicTaskListContent(
                state = state,
                onTaskClick = viewModel::onTaskClick,
                onDeleteTaskClick = { taskIdPendingDelete = it },
                onFlashcardsClick = viewModel::onFlashcardsClick,
                onRetry = viewModel::load,
            )
        }
    }
}
