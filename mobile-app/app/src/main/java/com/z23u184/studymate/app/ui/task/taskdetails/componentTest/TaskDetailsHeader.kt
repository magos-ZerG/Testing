package com.z23u184.studymate.app.ui.task.taskdetails.componentTest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.ui.task.component.formatDeadline
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import com.z23u184.studymate.app.ui.task.taskdetails.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
import com.z23u184.studymate.app.util.titleResId
import com.z23u184.studymate.domain.model.TaskStatus
@OptIn(ExperimentalTime::class)
@Composable
fun TaskDetailsHeader(
    title: String,
    status: TaskStatus,
    deadlineAt: Instant? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(text = stringResource(status.titleResId()), style = MaterialTheme.typography.bodyMedium)
        deadlineAt?.let {
            Text(
                text = stringResource(R.string.deadline_label, formatDeadline(it)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
