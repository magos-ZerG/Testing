package com.z23u184.studymate.app.ui.topic.topiclist

import com.z23u184.studymate.app.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z23u184.studymate.app.mapper.TopicUiMapper
import com.z23u184.studymate.app.navigation.AppDestinations
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.util.UiText
import com.z23u184.studymate.domain.model.UserMode
import com.z23u184.studymate.domain.usecase.GetAuthorizedUserIdUseCase
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.usecase.DeleteTopicUseCase
import com.z23u184.studymate.domain.usecase.GetTopicsUseCase
import com.z23u184.studymate.domain.usecase.GetUserModeUseCase
import com.z23u184.studymate.domain.usecase.LogoutUseCase
import com.z23u184.studymate.domain.usecase.PrepareLogoutUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class TopicListScreenViewModel(
    private val getTopicsUseCase: GetTopicsUseCase,
    private val deleteTopicUseCase: DeleteTopicUseCase,
    private val prepareLogoutUseCase: PrepareLogoutUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getUserModeUseCase: GetUserModeUseCase,
    private val getAuthorizedUserIdUseCase: GetAuthorizedUserIdUseCase,
    private val topicUiMapper: TopicUiMapper,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TopicListScreenUiState())
    val uiState: StateFlow<TopicListScreenUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()


    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val mode = getUserModeUseCase()
                val userId = getAuthorizedUserIdUseCase()
                val topics = getTopicsUseCase().map(topicUiMapper::map)
                TopicListScreenUiState(
                    isLoading = false,
                    topics = topics,
                    userDisplayName = userId,
                    isAuthorized = mode == UserMode.AUTHORIZED,
                    isLogoutInProgress = false,
                )
            }.onSuccess { _uiState.value = it }
                .onFailure {
                    _uiState.value = TopicListScreenUiState(
                        isLoading = false,
                        errorMessage = it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.topics_load_failed),
                        isLogoutInProgress = false,
                    )
                }
        }
    }

    fun onCreateTopicClick() { navigate(AppDestinations.CREATE_TOPIC) }
    fun onTopicClick(topicId: String) { navigate(AppDestinations.topicTasks(topicId)) }
    fun onLoginClick() { navigate(AppDestinations.LOGIN) }

    fun onConfirmDeleteTopic(topicId: String) {
        viewModelScope.launch {
            runCatching { deleteTopicUseCase(TopicId(topicId)) }
                .onSuccess { result ->
                    when (result) {
                        is com.z23u184.studymate.domain.DomainResult.Success -> {
                            _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.topic_deleted)))
                            load()
                        }
                        is com.z23u184.studymate.domain.DomainResult.Failure -> {
                            _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.topic_delete_failed)))
                        }
                    }
                }
                .onFailure {
                    _events.send(UiEvent.ShowSnackbar(it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.topic_delete_failed)))
                }
        }
    }

    fun onLogoutClick() {
        if (_uiState.value.isLogoutInProgress) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLogoutInProgress = true, errorMessage = null)

            runCatching {
                prepareLogoutUseCase()
                logoutUseCase()
            }.onSuccess {
                load()
                _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.logout_done)))
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLogoutInProgress = false)
                _events.send(
                    UiEvent.ShowSnackbar(
                        it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.logout_failed)
                    )
                )
            }
        }
    }

    private fun navigate(route: String) {
        viewModelScope.launch { _events.send(UiEvent.NavigateTo(route)) }
    }
}
