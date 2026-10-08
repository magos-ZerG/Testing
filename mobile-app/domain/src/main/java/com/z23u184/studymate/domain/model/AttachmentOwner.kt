package com.z23u184.studymate.domain.model

sealed interface AttachmentOwner {
    data class TaskDescription(
        val taskId: TaskId
    ) : AttachmentOwner

    data class TaskSolution(
        val taskId: TaskId
    ) : AttachmentOwner
}
