package com.z23u184.studymate.data.mapper

import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.TaskEntity
import com.z23u184.studymate.domain.model.StudyTask
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TaskStatus
import com.z23u184.studymate.domain.model.TopicId

object TaskEntityMapper {
    fun toDomain(entity: TaskEntity): StudyTask = StudyTask(
        id = TaskId(entity.id),
        topicId = TopicId(entity.topicId),
        title = entity.title,
        description = entity.description,
        status = TaskStatus.valueOf(entity.status),
        createdAt = entity.createdAtEpochMs.toInstantValue(),
        updatedAt = entity.updatedAtEpochMs.toInstantValue(),
        deadlineAt = entity.deadlineAtEpochMs?.toInstantValue()
    )

    fun fromDomain(
        model: StudyTask,
        remoteId: String? = null,
        isDeleted: Boolean = false,
        syncState: EntitySyncState = EntitySyncState.DIRTY
    ): TaskEntity = TaskEntity(
        id = model.id.value,
        topicId = model.topicId.value,
        title = model.title,
        description = model.description,
        status = model.status.name,
        createdAtEpochMs = model.createdAt.toEpochMillisValue(),
        updatedAtEpochMs = model.updatedAt.toEpochMillisValue(),
        deadlineAtEpochMs = model.deadlineAt?.toEpochMillisValue(),
        remoteId = remoteId,
        isDeleted = isDeleted,
        syncState = syncState
    )
}
