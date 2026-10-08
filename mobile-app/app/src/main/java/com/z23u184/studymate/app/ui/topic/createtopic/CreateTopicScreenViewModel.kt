package com.z23u184.studymate.app.ui.topic.createtopic

import com.z23u184.studymate.app.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.util.UiText
import com.z23u184.studymate.app.util.toUiText
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.usecase.CreateTopicUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class CreateTopicScreenViewModel(
    private val createTopicUseCase: CreateTopicUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateTopicScreenUiState())
    val uiState: StateFlow<CreateTopicScreenUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onTitleChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            title = value,
            titleError = null,
            saveError = null,
        )
    }

    fun onSaveClick() {
        val title = _uiState.value.title.trim()
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(
                titleError = UiText.StringResource(R.string.enter_topic_title),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, saveError = null)
            when (val result = createTopicUseCase(title)) {
                is DomainResult.Success -> {
                    _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.topic_created)))
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
}
