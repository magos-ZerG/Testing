package com.z23u184.studymate.app.ui.task.tasklist.componentTest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.R
import com.z23u184.studymate.app.ui.task.component.formatDeadline
import com.z23u184.studymate.app.ui.task.tasklist.*

import com.z23u184.studymate.app.util.titleResId
@Composable
fun TaskListItem(
    item: TaskListItemUi,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
            ) {
                Text(item.title)
                Text(stringResource(item.status.titleResId()))
                item.deadlineAt?.let {
                    Text(stringResource(R.string.deadline_label, formatDeadline(it)))
                }
                Text(if (item.hasSolution) stringResource(R.string.solution_added) else stringResource(R.string.solution_not_added))
            }
            TextButton(onClick = onDeleteClick) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}
