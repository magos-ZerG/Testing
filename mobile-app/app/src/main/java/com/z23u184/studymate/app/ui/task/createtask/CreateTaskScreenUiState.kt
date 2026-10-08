package com.z23u184.studymate.app.ui.task.createtask

import com.z23u184.studymate.app.util.UiText
import com.z23u184.studymate.domain.model.TaskStatus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class CreateTaskScreenUiState(
    val topicId: String = "",
    val title: String = "",
    val description: String = "",
    val selectedStatus: TaskStatus = TaskStatus.PLANNED,
    @OptIn(ExperimentalTime::class)
    val deadlineAt: Instant? = null,
    val isLoading: Boolean = false,
    val titleError: UiText? = null,
    val descriptionError: UiText? = null,
    val saveError: UiText? = null,
) {
    val isSaveEnabled: Boolean
        get() = title.isNotBlank()
}
