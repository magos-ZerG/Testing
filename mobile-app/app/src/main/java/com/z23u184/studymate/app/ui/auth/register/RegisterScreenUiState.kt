package com.z23u184.studymate.app.ui.auth.register

import com.z23u184.studymate.app.util.UiText

data class RegisterScreenUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmPasswordError: UiText? = null,
    val registerError: UiText? = null,
) {
    val isRegisterEnabled: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
}
