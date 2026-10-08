package com.z23u184.studymate.app.ui.task.taskdetails

import com.z23u184.studymate.app.R

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z23u184.studymate.app.mapper.SolutionUiMapper
import com.z23u184.studymate.app.navigation.NavArguments
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.util.UiText
import com.z23u184.studymate.app.util.toUiText
import com.z23u184.studymate.data.file.AttachmentFileManager
import com.z23u184.studymate.data.file.AttachmentRemoteFileDownloader
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.AttachmentId
import com.z23u184.studymate.domain.model.AttachmentOwner
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TaskStatus
import com.z23u184.studymate.domain.usecase.AddAttachmentUseCase
import com.z23u184.studymate.domain.usecase.DeleteAttachmentUseCase
import com.z23u184.studymate.domain.usecase.DeleteTaskSolutionUseCase
import com.z23u184.studymate.domain.usecase.GetAttachmentsByOwnerUseCase
import com.z23u184.studymate.domain.usecase.GetTaskByIdUseCase
import com.z23u184.studymate.domain.usecase.GetTaskSolutionUseCase
import com.z23u184.studymate.domain.usecase.SaveTaskSolutionUseCase
import com.z23u184.studymate.domain.usecase.UpdateTaskUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class TaskDetailsScreenViewModel(
    private val context: Context,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val getTaskSolutionUseCase: GetTaskSolutionUseCase,
    private val saveTaskSolutionUseCase: SaveTaskSolutionUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val getAttachmentsByOwnerUseCase: GetAttachmentsByOwnerUseCase,
    private val addAttachmentUseCase: AddAttachmentUseCase,
    private val deleteTaskSolutionUseCase: DeleteTaskSolutionUseCase,
    private val deleteAttachmentUseCase: DeleteAttachmentUseCase,
    private val solutionUiMapper: SolutionUiMapper,
    private val attachmentFileManager: AttachmentFileManager,
    private val attachmentRemoteFileDownloader: AttachmentRemoteFileDownloader,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val taskIdValue: String = savedStateHandle.get<String>(NavArguments.TASK_ID).orEmpty()
    private val taskId = TaskId(taskIdValue)
    private val _uiState = MutableStateFlow(TaskDetailsScreenUiState(taskId = taskIdValue))
    val uiState: StateFlow<TaskDetailsScreenUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        if (taskIdValue.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = UiText.StringResource(R.string.task_not_defined),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val task = getTaskByIdUseCase(taskId) ?: error("Вопрос не найден")
                val solution = getTaskSolutionUseCase(taskId)
                val descriptionAttachments = getAttachmentsByOwnerUseCase(AttachmentOwner.TaskDescription(task.id))
                    .map(solutionUiMapper::mapAttachment)
                val solutionAttachments = getAttachmentsByOwnerUseCase(AttachmentOwner.TaskSolution(task.id))
                    .map(solutionUiMapper::mapAttachment)
                TaskDetailsScreenUiState(
                    taskId = taskIdValue,
                    title = task.title,
                    description = task.description,
                    status = task.status,
                    deadlineAt = task.deadlineAt,
                    solutionText = solution?.content,
                    descriptionAttachments = descriptionAttachments,
                    solutionAttachments = solutionAttachments,
                    isLoading = false,
                )
            }.onSuccess { _uiState.value = it }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.load_error),
                    )
                }
        }
    }

    fun onBackClick() {
        viewModelScope.launch { _events.send(UiEvent.NavigateBack) }
    }

    fun onConfirmDeleteSolution() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val result = deleteTaskSolutionUseCase(taskId)) {
                is DomainResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.solution_deleted)))
                    load()
                }

                is DomainResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _events.send(UiEvent.ShowSnackbar(result.error.toUiText()))
                }
            }
        }
    }

    fun onConfirmDeleteAttachment(attachment: AttachmentUi) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val result = deleteAttachmentUseCase(AttachmentId(attachment.id))) {
                is DomainResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.file_deleted)))
                    load()
                }

                is DomainResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _events.send(UiEvent.ShowSnackbar(result.error.toUiText()))
                }
            }
        }
    }

    fun onSaveSolutionClick(contentValue: String) {
        val content = contentValue.trim()
        if (content.isBlank()) {
            viewModelScope.launch {
                _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.solution_text_empty_error)))
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (val result = saveTaskSolutionUseCase(taskId, content)) {
                is DomainResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.solution_saved)))
                    load()
                }

                is DomainResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _events.send(UiEvent.ShowSnackbar(result.error.toUiText()))
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun onSaveTaskClick(
        titleValue: String,
        descriptionValue: String,
        status: TaskStatus,
        deadlineAt: Instant?,
    ) {
        val title = titleValue.trim()
        val description = descriptionValue.trim()
        if (title.isBlank()) {
            viewModelScope.launch {
                _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.task_title_empty_error)))
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            when (
                val result = updateTaskUseCase(
                    taskId = taskId,
                    title = title,
                    description = description,
                    status = status,
                    deadlineAt = deadlineAt,
                )
            ) {
                is DomainResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.task_updated)))
                    load()
                }

                is DomainResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _events.send(UiEvent.ShowSnackbar(result.error.toUiText()))
                }
            }
        }
    }

    fun onAddDescriptionAttachment(uri: Uri) {
        addAttachment(uri, AttachmentOwner.TaskDescription(taskId))
    }

    fun onAddSolutionAttachment(uri: Uri) {
        if (!_uiState.value.hasSolution) {
            viewModelScope.launch {
                _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.add_solution_text_first)))
            }
            return
        }
        addAttachment(uri, AttachmentOwner.TaskSolution(taskId))
    }

    fun onAttachmentClick(attachment: AttachmentUi) {
        viewModelScope.launch {
            runCatching {
                val file = attachmentFileManager.getFileOrNull(attachment.localPath)
                    ?: downloadAttachmentFile(attachment)
                    ?: error("Файл вложения недоступен")

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                UiEvent.OpenAttachment(
                    uri = contentUri,
                    mimeType = attachment.mimeType.ifBlank { "application/octet-stream" },
                )
            }.onSuccess { _events.send(it) }
                .onFailure {
                    _events.send(UiEvent.ShowSnackbar(it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.attachment_open_failed)))
                }
        }
    }

    private fun addAttachment(uri: Uri, owner: AttachmentOwner) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            runCatching {
                val fileName = attachmentFileManager.queryDisplayName(uri).orEmpty().ifBlank {
                    "attachment_${System.currentTimeMillis()}"
                }
                val copiedFile = attachmentFileManager.ensureAppPrivateCopy(uri, fileName)
                val mimeType = attachmentFileManager.resolveMimeType(uri)
                    ?: attachmentFileManager.resolveMimeType(copiedFile)
                    ?: "application/octet-stream"
                val sizeBytes = attachmentFileManager.querySizeBytes(uri) ?: copiedFile.length()

                addAttachmentUseCase(
                    owner = owner,
                    fileName = copiedFile.name,
                    mimeType = mimeType,
                    sizeBytes = sizeBytes,
                    localPath = copiedFile.absolutePath,
                )
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(isSaving = false)
                when (result) {
                    is DomainResult.Success -> {
                        _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.attachment_added)))
                        load()
                    }

                    is DomainResult.Failure -> {
                        _events.send(UiEvent.ShowSnackbar(result.error.toUiText()))
                    }
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isSaving = false)
                _events.send(UiEvent.ShowSnackbar(it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.attachment_add_failed)))
            }
        }
    }

    private suspend fun downloadAttachmentFile(attachment: AttachmentUi): File? {
        val remoteId = attachment.remoteId ?: return null
        return attachmentRemoteFileDownloader.download(remoteId, attachment.fileName, attachment.id)
    }
}
