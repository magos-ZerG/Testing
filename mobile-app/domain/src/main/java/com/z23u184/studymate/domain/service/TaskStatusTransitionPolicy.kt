package com.z23u184.studymate.domain.service

import com.z23u184.studymate.domain.model.TaskStatus

interface TaskStatusTransitionPolicy {
    fun canMove(from: TaskStatus, to: TaskStatus): Boolean
}
