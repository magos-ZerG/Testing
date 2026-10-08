package com.z23u184.studymate.app.ui.auth.register

import com.z23u184.studymate.app.R

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z23u184.studymate.app.navigation.AppDestinations
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.util.UiText
import com.z23u184.studymate.app.util.isValidEmail
import com.z23u184.studymate.domain.usecase.RegisterUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class RegisterScreenViewModel(
    private val registerUseCase: RegisterUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterScreenUiState())
    val uiState: StateFlow<RegisterScreenUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChanged(value: String) = update { copy(email = value, emailError = null, registerError = null) }
    fun onPasswordChanged(value: String) = update { copy(password = value, passwordError = null, registerError = null) }
    fun onConfirmPasswordChanged(value: String) = update { copy(confirmPassword = value, confirmPasswordError = null, registerError = null) }

    fun onRegisterClick() {
        val state = _uiState.value
        val email = state.email.trim()
        val emailError = if (!isValidEmail(email)) UiText.StringResource(R.string.enter_valid_email) else null
        val passwordError = if (state.password.length < 8) UiText.StringResource(R.string.password_too_short) else null
        val confirmError = if (state.password != state.confirmPassword) UiText.StringResource(R.string.passwords_do_not_match) else null
        if (emailError != null || passwordError != null || confirmError != null) {
            _uiState.value = state.copy(
                emailError = emailError,
                passwordError = passwordError,
                confirmPasswordError = confirmError,
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, registerError = null)
            runCatching { registerUseCase(email, state.password) }
                .onSuccess {
                    _events.send(UiEvent.ShowSnackbar(UiText.StringResource(R.string.registration_success)))
                    _events.send(UiEvent.NavigateTo(AppDestinations.LOGIN))
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        registerError = it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.register_error),
                    )
                }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun onBackToLogin() {
        viewModelScope.launch { _events.send(UiEvent.NavigateBack) }
    }

    private inline fun update(block: RegisterScreenUiState.() -> RegisterScreenUiState) {
        _uiState.value = _uiState.value.block()
    }
}
