package com.z23u184.studymate.app.ui.task.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.time.ExperimentalTime
import com.z23u184.studymate.app.R
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DeadlinePickerField(
    deadlineAt: Instant?,
    onDeadlineChanged: (Instant?) -> Unit,
) {
    var isDialogVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = deadlineAt?.let { stringResource(R.string.deadline_label, formatDeadline(it)) }
                ?: stringResource(R.string.deadline_not_set)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { isDialogVisible = true }) {
                Text(if (deadlineAt == null) stringResource(R.string.choose_deadline) else stringResource(R.string.change_deadline))
            }

            if (deadlineAt != null) {
                TextButton(onClick = { onDeadlineChanged(null) }) {
                    Text(stringResource(R.string.remove_deadline))
                }
            }
        }
    }

    if (isDialogVisible) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadlineAt?.toEpochMilliseconds()
        )

        DatePickerDialog(
            onDismissRequest = { isDialogVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        onDeadlineChanged(
                            selectedMillis?.let { Instant.fromEpochMilliseconds(it) }
                        )
                        isDialogVisible = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDialogVisible = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
