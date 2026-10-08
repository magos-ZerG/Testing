package com.z23u184.studymate.app.mapper

import com.z23u184.studymate.app.ui.task.tasklist.TaskListItemUi
import com.z23u184.studymate.domain.model.StudyTask

class TaskUiMapper {
    fun map(task: StudyTask, hasSolution: Boolean = false): TaskListItemUi = TaskListItemUi(
        id = task.id.value,
        title = task.title,
        status = task.status,
        deadlineAt = task.deadlineAt,
        hasSolution = hasSolution,
        updatedAtText = task.updatedAt.toString(),
    )
}
