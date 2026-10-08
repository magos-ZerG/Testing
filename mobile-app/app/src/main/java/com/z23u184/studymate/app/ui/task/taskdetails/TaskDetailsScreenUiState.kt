package com.z23u184.studymate.app.ui.task.taskdetails

import com.z23u184.studymate.app.util.UiText
import com.z23u184.studymate.domain.model.TaskStatus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class TaskDetailsScreenUiState(
    val taskId: String = "",
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.PLANNED,
    @OptIn(ExperimentalTime::class)
    val deadlineAt: Instant? = null,
    val solutionText: String? = null,
    val descriptionAttachments: List<AttachmentUi> = emptyList(),
    val solutionAttachments: List<AttachmentUi> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: UiText? = null,
) {
    val hasSolution: Boolean
        get() = solutionText != null
}

data class AttachmentUi(
    val id: String,
    val remoteId: String?,
    val fileName: String,
    val mimeType: String,
    val sizeText: String,
    val localPath: String?,
)
