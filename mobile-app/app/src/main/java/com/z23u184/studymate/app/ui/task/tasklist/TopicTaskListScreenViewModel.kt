package com.z23u184.studymate.app.ui.task.tasklist

import com.z23u184.studymate.app.R

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z23u184.studymate.app.mapper.TaskUiMapper
import com.z23u184.studymate.app.navigation.AppDestinations
import com.z23u184.studymate.app.navigation.NavArguments
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.util.UiText
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.usecase.DeleteTaskUseCase
import com.z23u184.studymate.domain.usecase.GetTaskSolutionUseCase
import com.z23u184.studymate.domain.usecase.GetTasksByTopicUseCase
import com.z23u184.studymate.domain.usecase.GetTopicByIdUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class TopicTaskListScreenViewModel(
    private val getTopicByIdUseCase: GetTopicByIdUseCase,
    private val getTasksByTopicUseCase: GetTasksByTopicUseCase,
    private val getTaskSolutionUseCase: GetTaskSolutionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val taskUiMapper: TaskUiMapper,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val topicId: String = savedStateHandle.get<String>(NavArguments.TOPIC_ID).orEmpty()
    private val _uiState = MutableStateFlow(TopicTaskListScreenUiState(topicId = topicId))
    val uiState: StateFlow<TopicTaskListScreenUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()


    fun load() {
        if (topicId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = UiText.StringResource(R.string.topic_not_defined),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val topic = getTopicByIdUseCase(TopicId(topicId)) ?: error("Тема не найдена")
                val tasks = getTasksByTopicUseCase(topic.id).map { task ->
                    val hasSolution = getTaskSolutionUseCase(task.id) != null
                    taskUiMapper.map(task, hasSolution)
                }
                TopicTaskListScreenUiState(
                    topicId = topicId,
                    topicTitle = topic.title,
                    isLoading = false,
                    tasks = tasks,
                )
            }.onSuccess { _uiState.value = it }
             .onFailure {
                 _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.load_error))
             }
        }
    }

    fun onCreateTaskClick() { navigate(AppDestinations.createTask(topicId)) }
    fun onFlashcardsClick() { navigate(AppDestinations.flashcards(topicId)) }
    fun onTaskClick(taskId: String) { navigate(AppDestinations.taskDetails(taskId)) }

    fun onConfirmDeleteTask(taskId: String) {
        viewModelScope.launch {
            runCatching { deleteTaskUseCase(TaskId(taskId)) }
                .onSuccess { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.task_deleted)))
                            load()
                        }
                        is DomainResult.Failure -> {
                            _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.task_delete_failed)))
                        }
                    }
                }
                .onFailure {
                    _events.send(UiEvent.ShowSnackbar(it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.task_delete_failed)))
                }
        }
    }

    fun onBackClick() { viewModelScope.launch { _events.send(UiEvent.NavigateBack) } }

    private fun navigate(route: String) {
        viewModelScope.launch { _events.send(UiEvent.NavigateTo(route)) }
    }
}
