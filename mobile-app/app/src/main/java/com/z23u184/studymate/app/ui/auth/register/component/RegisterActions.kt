package com.z23u184.studymate.app.ui.auth.register.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.ui.common.component.PrimaryButton
import com.z23u184.studymate.app.ui.auth.register.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun RegisterActions(
    state: RegisterScreenUiState,
    onRegisterClick: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        PrimaryButton(stringResource(R.string.register_action), enabled = state.isRegisterEnabled, loading = state.isLoading, onClick = onRegisterClick)
        TextButton(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back_to_login)) }
    }
}
