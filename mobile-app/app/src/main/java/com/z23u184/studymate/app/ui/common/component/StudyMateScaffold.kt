package com.z23u184.studymate.app.ui.common.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun StudyMateScaffold(
    snackbarHostState: SnackbarHostState,
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    floatingActionIcon: ImageVector? = null,
    floatingActionContentDescription: String? = null,
    onFloatingActionClick: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            StudyMateTopBar(
                title = title,
                onBackClick = onBackClick,
                actions = actions,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (floatingActionIcon != null && onFloatingActionClick != null) {
                FloatingActionButton(onClick = onFloatingActionClick) {
                    Icon(floatingActionIcon, contentDescription = floatingActionContentDescription)
                }
            }
        },
        content = content,
    )
}
