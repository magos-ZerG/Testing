package com.z23u184.studymate.app.ui.auth.login

import com.z23u184.studymate.app.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z23u184.studymate.app.navigation.AppDestinations
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.util.UiText
import com.z23u184.studymate.app.util.isValidEmail
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.usecase.HandleLoginSyncUseCase
import com.z23u184.studymate.domain.usecase.LoginUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class LoginScreenViewModel(
    private val loginUseCase: LoginUseCase,
    private val handleLoginSyncUseCase: HandleLoginSyncUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginScreenUiState())
    val uiState: StateFlow<LoginScreenUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChanged(value: String) {
        _uiState.value = _uiState.value.copy(email = value, emailError = null, authError = null)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null, authError = null)
    }

    fun onLoginClick() {
        val state = _uiState.value
        val email = state.email.trim()
        val password = state.password
        val emailError = if (!isValidEmail(email)) UiText.StringResource(R.string.enter_valid_email) else null
        val passwordError = if (password.length < 4) UiText.StringResource(R.string.password_too_short) else null

        if (emailError != null || passwordError != null) {
            _uiState.value = state.copy(emailError = emailError, passwordError = passwordError)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, authError = null)
            runCatching { loginUseCase(email, password) }
                .onSuccess {
                    when (val syncResult = handleLoginSyncUseCase()) {
                        is DomainResult.Success -> Unit
                        is DomainResult.Failure -> {
                            _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.login_sync_not_started)))
                        }
                    }
                    _events.send(UiEvent.NavigateTo(AppDestinations.TOPICS))
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        authError = it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.login_error),
                    )
                }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onRegisterClick() {
        viewModelScope.launch { _events.send(UiEvent.NavigateTo(AppDestinations.REGISTER)) }
    }
}
