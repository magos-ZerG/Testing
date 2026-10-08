package com.z23u184.studymate.data.mapper

import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.TaskSolutionEntity
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TaskSolution
import com.z23u184.studymate.domain.model.TaskSolutionId

object TaskSolutionEntityMapper {
    fun toDomain(entity: TaskSolutionEntity): TaskSolution = TaskSolution(
        id = TaskSolutionId(entity.id),
        taskId = TaskId(entity.taskId),
        content = entity.content,
        updatedAt = entity.updatedAtEpochMs.toInstantValue()
    )

    fun fromDomain(
        model: TaskSolution,
        remoteId: String? = null,
        isDeleted: Boolean = false,
        syncState: EntitySyncState = EntitySyncState.DIRTY
    ): TaskSolutionEntity = TaskSolutionEntity(
        id = model.id.value,
        taskId = model.taskId.value,
        content = model.content,
        updatedAtEpochMs = model.updatedAt.toEpochMillisValue(),
        remoteId = remoteId,
        isDeleted = isDeleted,
        syncState = syncState
    )
}
