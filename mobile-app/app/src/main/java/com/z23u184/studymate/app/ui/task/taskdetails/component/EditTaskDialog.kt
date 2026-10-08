package com.z23u184.studymate.app.ui.task.taskdetails.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.R
import com.z23u184.studymate.app.ui.task.component.DeadlinePickerField
import com.z23u184.studymate.app.ui.task.component.TaskStatusSelector
import com.z23u184.studymate.domain.model.TaskStatus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import com.z23u184.studymate.app.ui.task.taskdetails.*

@OptIn(ExperimentalTime::class)
@Composable
fun EditTaskDialog(
    title: String,
    description: String,
    status: TaskStatus,
    deadlineAt: Instant?,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onDeadlineChange: (Instant?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_task_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = onTitleChange, label = { Text(stringResource(R.string.title)) })
                OutlinedTextField(value = description, onValueChange = onDescriptionChange, label = { Text(stringResource(R.string.task_description)) })
                TaskStatusSelector(
                    selectedStatus = status,
                    onStatusSelected = onStatusChange,
                )

                DeadlinePickerField(
                    deadlineAt = deadlineAt,
                    onDeadlineChanged = onDeadlineChange,
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
