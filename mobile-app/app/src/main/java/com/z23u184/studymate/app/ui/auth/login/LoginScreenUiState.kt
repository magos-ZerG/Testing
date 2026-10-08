package com.z23u184.studymate.app.ui.auth.login

import com.z23u184.studymate.app.util.UiText

data class LoginScreenUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val authError: UiText? = null,
) {
    val isLoginEnabled: Boolean
        get() = email.isNotBlank() && password.isNotBlank()
}
