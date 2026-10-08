package com.z23u184.studymate.app.ui.auth.login.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.ui.auth.login.*
import com.z23u184.studymate.app.ui.common.component.FormTextField

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun LoginForm(
    state: LoginScreenUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        FormTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            label = stringResource(R.string.email),
            errorText = state.emailError?.asString(),
        )
        FormTextField(
            value = state.password,
            onValueChange = onPasswordChanged,
            label = stringResource(R.string.password),
            errorText = state.passwordError?.asString(),
            isPassword = true,
        )
        state.authError?.let {
            Text(
                text = it.asString(),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
