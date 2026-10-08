package com.z23u184.studymate.app.ui.task.taskdetails

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.z23u184.studymate.app.R
import com.z23u184.studymate.app.navigation.AppDestinations
import com.z23u184.studymate.app.navigation.popBackStackOrNavigate
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.ui.common.component.ErrorContent
import com.z23u184.studymate.app.ui.common.component.ConfirmDialog
import com.z23u184.studymate.app.ui.common.component.LoadingContent
import com.z23u184.studymate.app.ui.common.component.StudyMateScaffold
import com.z23u184.studymate.domain.model.TaskStatus
import org.koin.androidx.compose.koinViewModel
import com.z23u184.studymate.app.ui.task.taskdetails.component.*

@Composable
fun TaskDetailsScreen(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    viewModel: TaskDetailsScreenViewModel = koinViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var attachmentPendingDelete by remember { mutableStateOf<AttachmentUi?>(null) }
    var isDeleteSolutionDialogVisible by remember { mutableStateOf(false) }
    var isEditSolutionDialogVisible by remember { mutableStateOf(false) }
    var editableSolutionText by remember { mutableStateOf("") }
    var isEditTaskDialogVisible by remember { mutableStateOf(false) }
    var editableTaskTitle by remember { mutableStateOf("") }
    var editableTaskDescription by remember { mutableStateOf("") }
    var editableTaskStatus by remember { mutableStateOf(TaskStatus.PLANNED) }
    var editableTaskDeadlineAt by remember { mutableStateOf(state.deadlineAt) }
    val descriptionAttachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::onAddDescriptionAttachment)
    }
    val solutionAttachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::onAddSolutionAttachment)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.NavigateTo -> navController.navigate(event.route)
                UiEvent.NavigateBack -> navController.popBackStackOrNavigate(AppDestinations.TOPICS)
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message.asString())
                is UiEvent.OpenAttachment -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                        .setDataAndType(event.uri, event.mimeType)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent)
                }
            }
        }
    }

    attachmentPendingDelete?.let { attachment ->
        ConfirmDialog(
            title = stringResource(R.string.delete_file_question),
            text = stringResource(R.string.delete_file_text, attachment.fileName),
            onConfirm = {
                attachmentPendingDelete = null
                viewModel.onConfirmDeleteAttachment(attachment)
            },
            onDismiss = { attachmentPendingDelete = null },
        )
    }

    if (isDeleteSolutionDialogVisible) {
        ConfirmDialog(
            title = stringResource(R.string.delete_solution_question),
            text = stringResource(R.string.delete_solution_text),
            onConfirm = {
                isDeleteSolutionDialogVisible = false
                viewModel.onConfirmDeleteSolution()
            },
            onDismiss = { isDeleteSolutionDialogVisible = false },
        )
    }

    if (isEditSolutionDialogVisible) {
        EditSolutionDialog(
            value = editableSolutionText,
            onValueChange = { editableSolutionText = it },
            onConfirm = {
                if (editableSolutionText.isNotBlank()) {
                    isEditSolutionDialogVisible = false
                }
                viewModel.onSaveSolutionClick(editableSolutionText)
            },
            onDismiss = { isEditSolutionDialogVisible = false },
        )
    }
    if (isEditTaskDialogVisible) {
        EditTaskDialog(
            title = editableTaskTitle,
            description = editableTaskDescription,
            status = editableTaskStatus,
            deadlineAt = editableTaskDeadlineAt,
            onTitleChange = { editableTaskTitle = it },
            onDescriptionChange = { editableTaskDescription = it },
            onStatusChange = { editableTaskStatus = it },
            onDeadlineChange = { editableTaskDeadlineAt = it },
            onConfirm = {
                if (editableTaskTitle.isNotBlank()) {
                    isEditTaskDialogVisible = false
                }
                viewModel.onSaveTaskClick(
                    titleValue = editableTaskTitle,
                    descriptionValue = editableTaskDescription,
                    status = editableTaskStatus,
                    deadlineAt = editableTaskDeadlineAt,
                )
            },
            onDismiss = { isEditTaskDialogVisible = false },
        )
    }

    StudyMateScaffold(snackbarHostState = snackbarHostState, title = stringResource(R.string.view_task_title), onBackClick = viewModel::onBackClick) { padding ->
        when {
            state.isLoading -> LoadingContent()
            state.errorMessage != null -> ErrorContent(state.errorMessage.asString(), viewModel::load)
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TaskDetailsHeader(
                    title = state.title,
                    status = state.status,
                    deadlineAt = state.deadlineAt,
                )
                TaskDescriptionBlock(
                    description = state.description,
                    attachments = state.descriptionAttachments,
                    onAddAttachmentClick = { descriptionAttachmentPicker.launch("*/*") },
                    onAttachmentClick = viewModel::onAttachmentClick,
                    onDeleteAttachmentClick = { attachmentPendingDelete = it },
                )
                TaskSolutionBlock(
                    solutionText = state.solutionText,
                    attachments = state.solutionAttachments,
                    hasSolution = state.hasSolution,
                    onAddAttachmentClick = { solutionAttachmentPicker.launch("*/*") },
                    onAttachmentClick = viewModel::onAttachmentClick,
                    onDeleteAttachmentClick = { attachmentPendingDelete = it },
                )
                TaskDetailsActions(
                    hasSolution = state.hasSolution,
                    onEditSolutionClick = {
                        editableSolutionText = state.solutionText.orEmpty()
                        isEditSolutionDialogVisible = true
                    },
                    onDeleteSolutionClick = {
                        if (state.hasSolution) {
                            isDeleteSolutionDialogVisible = true
                        }
                    },
                    onEditTaskClick = {
                        editableTaskTitle = state.title
                        editableTaskDescription = state.description
                        editableTaskStatus = runCatching { state.status }.getOrDefault(TaskStatus.PLANNED)
                        editableTaskDeadlineAt = state.deadlineAt
                        isEditTaskDialogVisible = true
                    },
                )
            }
        }
    }
}
