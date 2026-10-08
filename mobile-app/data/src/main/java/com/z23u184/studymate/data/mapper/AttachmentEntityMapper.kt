package com.z23u184.studymate.data.mapper

import com.z23u184.studymate.data.db.entity.AttachmentEntity
import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.OwnerType
import com.z23u184.studymate.domain.model.Attachment
import com.z23u184.studymate.domain.model.AttachmentId
import com.z23u184.studymate.domain.model.AttachmentOwner
import com.z23u184.studymate.domain.model.TaskId

object AttachmentEntityMapper {
    fun toDomain(entity: AttachmentEntity): Attachment = Attachment(
        id = AttachmentId(entity.id),
        owner = when (entity.ownerType) {
            OwnerType.TASK_DESCRIPTION -> AttachmentOwner.TaskDescription(TaskId(entity.ownerTaskId))
            OwnerType.TASK_SOLUTION -> AttachmentOwner.TaskSolution(TaskId(entity.ownerTaskId))
        },
        fileName = entity.fileName,
        mimeType = entity.mimeType,
        sizeBytes = entity.sizeBytes,
        createdAt = entity.createdAtEpochMs.toInstantValue(),
        updatedAt = entity.updatedAtEpochMs.toInstantValue(),
        localPath = entity.localPath,
        remoteId = entity.remoteId,
        remoteFileId = entity.remoteFileId,
        uploadState = entity.uploadState
    )

    fun fromDomain(
        model: Attachment,
        remoteId: String? = null,
        isDeleted: Boolean = false,
        syncState: EntitySyncState = EntitySyncState.DIRTY,
        updatedAtEpochMs: Long = model.updatedAt.toEpochMillisValue(),
    ): AttachmentEntity {
        val (ownerType, ownerTaskId) = when (val owner = model.owner) {
            is AttachmentOwner.TaskDescription -> OwnerType.TASK_DESCRIPTION to owner.taskId.value
            is AttachmentOwner.TaskSolution -> OwnerType.TASK_SOLUTION to owner.taskId.value
        }

        return AttachmentEntity(
            id = model.id.value,
            ownerType = ownerType,
            ownerTaskId = ownerTaskId,
            fileName = model.fileName,
            mimeType = model.mimeType,
            sizeBytes = model.sizeBytes,
            createdAtEpochMs = model.createdAt.toEpochMillisValue(),
            updatedAtEpochMs = updatedAtEpochMs,
            localPath = model.localPath,
            remoteFileId = model.remoteFileId,
            uploadState = model.uploadState,
            remoteId = remoteId,
            isDeleted = isDeleted,
            syncState = syncState
        )
    }
}
