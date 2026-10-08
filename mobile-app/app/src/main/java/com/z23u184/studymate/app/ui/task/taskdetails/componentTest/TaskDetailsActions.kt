package com.z23u184.studymate.app.ui.task.taskdetails.componentTest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.ui.task.taskdetails.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun TaskDetailsActions(
    hasSolution: Boolean,
    onEditSolutionClick: () -> Unit,
    onDeleteSolutionClick: () -> Unit,
    onEditTaskClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onEditSolutionClick, modifier = Modifier.fillMaxWidth()) {
            Text(if (hasSolution) stringResource(R.string.edit_solution_action) else stringResource(R.string.add_solution_action))
        }
        if (hasSolution) {
            OutlinedButton(onClick = onDeleteSolutionClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.delete_solution))
            }
        }
        Button(onClick = onEditTaskClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.edit_task_action))
        }
    }
}
