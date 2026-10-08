package com.z23u184.studymate.app.ui.common.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PrimaryButton(
    text: String,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled && !loading, modifier = Modifier.fillMaxWidth()) {
        if (loading) CircularProgressIndicator() else Text(text)
    }
}
