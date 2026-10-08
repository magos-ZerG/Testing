package com.z23u184.studymate.app.ui.task.createtask.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.ui.common.component.FormTextField
import com.z23u184.studymate.app.ui.task.component.DeadlinePickerField
import kotlin.time.Instant
import com.z23u184.studymate.app.ui.task.createtask.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun CreateTaskForm(
    state: CreateTaskScreenUiState,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDeadlineChanged: (Instant?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormTextField(state.title, onTitleChanged, stringResource(R.string.task_title), state.titleError?.asString())
        FormTextField(state.description, onDescriptionChanged, stringResource(R.string.task_description), state.descriptionError?.asString(), singleLine = false)
        DeadlinePickerField(
            deadlineAt = state.deadlineAt,
            onDeadlineChanged = onDeadlineChanged,
        )
        state.saveError?.let { Text(it.asString()) }
    }
}
