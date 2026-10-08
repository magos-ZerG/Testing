package com.z23u184.studymate.app.ui.task.taskdetails.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.R
import com.z23u184.studymate.app.ui.common.component.MainInfoCard
import com.z23u184.studymate.app.ui.task.component.formatDeadline
import com.z23u184.studymate.app.ui.task.taskdetails.*
import com.z23u184.studymate.app.util.titleResId
import com.z23u184.studymate.domain.model.TaskStatus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsHeader(
    title: String,
    status: TaskStatus,
    deadlineAt: Instant? = null,
) {
    MainInfoCard {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text(stringResource(status.titleResId())) })
            deadlineAt?.let {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.deadline_label, formatDeadline(it))) })
            }
        }
    }
}
