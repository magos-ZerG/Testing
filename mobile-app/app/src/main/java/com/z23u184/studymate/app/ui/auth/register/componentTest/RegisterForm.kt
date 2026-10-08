package com.z23u184.studymate.app.ui.auth.register.componentTest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.ui.common.component.FormTextField
import com.z23u184.studymate.app.ui.auth.register.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun RegisterForm(
    state: RegisterScreenUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormTextField(state.email, onEmailChanged, stringResource(R.string.email), state.emailError?.asString())
        FormTextField(state.password, onPasswordChanged, stringResource(R.string.password), state.passwordError?.asString(), isPassword = true)
        FormTextField(state.confirmPassword, onConfirmPasswordChanged, stringResource(R.string.confirm_password), state.confirmPasswordError?.asString(), isPassword = true)
        state.registerError?.let { Text(it.asString()) }
    }
}
