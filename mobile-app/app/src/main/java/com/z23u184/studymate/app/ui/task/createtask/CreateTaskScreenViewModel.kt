package com.z23u184.studymate.app.ui.task.createtask

import com.z23u184.studymate.app.R

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z23u184.studymate.app.navigation.NavArguments
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.util.UiText
import com.z23u184.studymate.app.util.toUiText
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.TaskStatus
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.usecase.CreateTaskUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class CreateTaskScreenViewModel(
    private val createTaskUseCase: CreateTaskUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val topicId: String = savedStateHandle.get<String>(NavArguments.TOPIC_ID).orEmpty()
    private val _uiState = MutableStateFlow(CreateTaskScreenUiState(topicId = topicId))
    val uiState: StateFlow<CreateTaskScreenUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onTitleChanged(value: String) = update { copy(title = value, titleError = null, saveError = null) }
    fun onDescriptionChanged(value: String) = update { copy(description = value, descriptionError = null, saveError = null) }

    fun onStatusChanged(value: TaskStatus) = update {
        copy(selectedStatus = value, saveError = null)
    }

    @OptIn(ExperimentalTime::class)
    fun onDeadlineChanged(value: Instant?) = update {
        copy(deadlineAt = value, saveError = null)
    }

    fun onClearDeadlineClick() = update {
        copy(deadlineAt = null, saveError = null)
    }

    fun onSaveClick() {
        val currentState = _uiState.value
        val title = currentState.title.trim()
        val description = currentState.description.trim()

        if (topicId.isBlank()) {
            _uiState.value = currentState.copy(saveError = UiText.StringResource(R.string.topic_not_defined))
            return
        }
        if (title.isBlank()) {
            _uiState.value = currentState.copy(titleError = UiText.StringResource(R.string.enter_task_title))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, saveError = null)
            when (
                val result = createTaskUseCase(
                    topicId = TopicId(topicId),
                    title = title,
                    description = description,
                    status = currentState.selectedStatus,
                    deadlineAt = currentState.deadlineAt,
                )
            ) {
                is DomainResult.Success -> {
                    _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.task_created)))
                    _events.send(UiEvent.NavigateBack)
                }
                is DomainResult.Failure -> {
                    _uiState.value = _uiState.value.copy(saveError = result.error.toUiText())
                }
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onBackClick() {
        viewModelScope.launch { _events.send(UiEvent.NavigateBack) }
    }

    private inline fun update(block: CreateTaskScreenUiState.() -> CreateTaskScreenUiState) {
        _uiState.value = _uiState.value.block()
    }
}
