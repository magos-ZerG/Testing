package com.z23u184.studymate.app.ui.task.taskdetails.componentTest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.ui.common.component.AttachmentChip
import com.z23u184.studymate.app.ui.common.component.SecondaryButton
import com.z23u184.studymate.app.ui.task.taskdetails.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun TaskDescriptionBlock(
    description: String,
    attachments: List<AttachmentUi>,
    onAddAttachmentClick: () -> Unit,
    onAttachmentClick: (AttachmentUi) -> Unit,
    onDeleteAttachmentClick: (AttachmentUi) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = stringResource(R.string.condition), style = MaterialTheme.typography.titleMedium)
        Text(text = description.ifBlank { stringResource(R.string.description_not_added) })
        SecondaryButton(text = stringResource(R.string.add_condition_attachment), onClick = onAddAttachmentClick)
        attachments.forEach {
            AttachmentChip(
                fileName = it.fileName,
                sizeText = it.sizeText,
                onClick = { onAttachmentClick(it) },
                onDeleteClick = { onDeleteAttachmentClick(it) },
            )
        }
    }
}
