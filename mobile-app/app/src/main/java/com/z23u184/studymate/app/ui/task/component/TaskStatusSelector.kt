package com.z23u184.studymate.app.ui.task.component

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
import com.z23u184.studymate.app.util.titleResId
import com.z23u184.studymate.domain.model.TaskStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskStatusSelector(
    selectedStatus: TaskStatus,
    onStatusSelected: (TaskStatus) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = stringResource(selectedStatus.titleResId()),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.task_status)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.menuAnchor(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            TaskStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(stringResource(status.titleResId())) },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    },
                )
            }
        }
    }
}
