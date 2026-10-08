package com.z23u184.studymate.app.ui.auth.login.componentTest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.ui.common.component.PrimaryButton
import com.z23u184.studymate.app.ui.auth.login.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun LoginActions(
    state: LoginScreenUiState,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        PrimaryButton(
            text = stringResource(R.string.login),
            enabled = state.isLoginEnabled,
            loading = state.isLoading,
            onClick = onLoginClick,
        )
        TextButton(onClick = onRegisterClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.register))
        }
    }
}
