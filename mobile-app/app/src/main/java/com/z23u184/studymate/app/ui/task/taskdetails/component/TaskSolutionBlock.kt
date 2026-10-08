package com.z23u184.studymate.app.ui.task.taskdetails.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.R
import com.z23u184.studymate.app.ui.common.component.AttachmentChip
import com.z23u184.studymate.app.ui.common.component.MainInfoCard
import com.z23u184.studymate.app.ui.common.component.MainSearchField
import com.z23u184.studymate.app.ui.common.component.SecondaryButton
import com.z23u184.studymate.app.ui.task.taskdetails.*

@Composable
fun TaskSolutionBlock(
    solutionText: String?,
    attachments: List<AttachmentUi>,
    hasSolution: Boolean,
    onAddAttachmentClick: () -> Unit,
    onAttachmentClick: (AttachmentUi) -> Unit,
    onDeleteAttachmentClick: (AttachmentUi) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredAttachments = attachments.filter { it.fileName.contains(searchQuery, ignoreCase = true) }

    MainInfoCard {
        Text(text = stringResource(R.string.solution), style = MaterialTheme.typography.titleMedium)
        Text(
            text = solutionText ?: stringResource(R.string.solution_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = if (!hasSolution) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        SecondaryButton(
            text = stringResource(R.string.add_solution_attachment),
            onClick = onAddAttachmentClick,
            enabled = hasSolution,
        )
        if (!hasSolution) {
            Text(
                text = stringResource(R.string.save_solution_before_files),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (attachments.isNotEmpty()) {
            MainSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.file_search_placeholder),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (filteredAttachments.isEmpty()) {
                    Text(stringResource(R.string.files_not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    filteredAttachments.forEach {
                        AttachmentChip(
                            fileName = it.fileName,
                            sizeText = it.sizeText,
                            onClick = { onAttachmentClick(it) },
                            onDeleteClick = { onDeleteAttachmentClick(it) },
                        )
                    }
                }
            }
        }
    }
}
