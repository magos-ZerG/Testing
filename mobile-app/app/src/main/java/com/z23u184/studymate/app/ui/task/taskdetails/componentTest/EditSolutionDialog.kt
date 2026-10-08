package com.z23u184.studymate.app.ui.task.taskdetails.componentTest

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.z23u184.studymate.app.ui.task.taskdetails.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun EditSolutionDialog(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_solution_title)) },
        text = { OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(stringResource(R.string.solution)) }) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
