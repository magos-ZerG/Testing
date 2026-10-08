package com.z23u184.studymate.domain.service

import com.z23u184.studymate.domain.model.TaskStatus

class DefaultTaskStatusTransitionPolicy : TaskStatusTransitionPolicy {
    override fun canMove(from: TaskStatus, to: TaskStatus): Boolean {
        if (from == to) return true

        return when (from) {
            TaskStatus.PLANNED -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.ARCHIVED
            TaskStatus.IN_PROGRESS -> to == TaskStatus.DONE || to == TaskStatus.PLANNED || to == TaskStatus.ARCHIVED
            TaskStatus.DONE -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.ARCHIVED
            TaskStatus.ARCHIVED -> to == TaskStatus.PLANNED
        }
    }
}
