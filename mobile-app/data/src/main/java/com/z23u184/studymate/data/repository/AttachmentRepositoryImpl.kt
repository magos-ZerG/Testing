package com.z23u184.studymate.data.repository

import com.z23u184.studymate.data.datasource.local.AttachmentLocalDataSource
import com.z23u184.studymate.data.datasource.local.SyncQueueLocalDataSource
import com.z23u184.studymate.data.db.StudyMateDatabase
import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.OwnerType
import com.z23u184.studymate.data.db.entity.SyncEntityType
import com.z23u184.studymate.data.db.entity.SyncOperation
import com.z23u184.studymate.data.mapper.AttachmentEntityMapper
import com.z23u184.studymate.data.sync.queue.SyncQueuePlanner
import com.z23u184.studymate.domain.model.Attachment
import com.z23u184.studymate.domain.model.AttachmentId
import com.z23u184.studymate.domain.model.AttachmentOwner
import com.z23u184.studymate.domain.repository.AttachmentRepository

class AttachmentRepositoryImpl(
    database: StudyMateDatabase,
    private val attachmentLocalDataSource: AttachmentLocalDataSource,
    syncQueueLocalDataSource: SyncQueueLocalDataSource,
    syncQueuePlanner: SyncQueuePlanner
) : BaseQueueingRepository(database, syncQueueLocalDataSource, syncQueuePlanner), AttachmentRepository {

    override suspend fun getById(id: AttachmentId): Attachment? = attachmentLocalDataSource.getById(id.value)
        ?.takeIf { !it.isDeleted }
        ?.let(AttachmentEntityMapper::toDomain)

    override suspend fun getByOwner(owner: AttachmentOwner): List<Attachment> {
        val (type, taskId) = owner.toStorage()
        return attachmentLocalDataSource.getByOwner(type, taskId).map(AttachmentEntityMapper::toDomain)
    }

    override suspend fun save(attachment: Attachment) {
        val existing = attachmentLocalDataSource.getById(attachment.id.value)
        val operation = if (existing == null) SyncOperation.CREATE else SyncOperation.UPDATE
        val updatedAt = System.currentTimeMillis()
        inTransaction {
            attachmentLocalDataSource.save(
                AttachmentEntityMapper.fromDomain(
                    attachment,
                    remoteId = existing?.remoteId,
                    syncState = EntitySyncState.DIRTY,
                    updatedAtEpochMs = updatedAt,
                )
            )
            enqueueMutation(SyncEntityType.ATTACHMENT, attachment.id.value, operation, updatedAt)
        }
    }

    override suspend fun delete(id: AttachmentId) {
        val existing = attachmentLocalDataSource.getById(id.value) ?: return
        val now = System.currentTimeMillis()
        inTransaction {
            attachmentLocalDataSource.softDelete(id.value, now)
            enqueueMutation(SyncEntityType.ATTACHMENT, id.value, SyncOperation.DELETE, now)
        }
    }

    override suspend fun deleteByOwner(owner: AttachmentOwner) {
        val (type, taskId) = owner.toStorage()
        val existing = attachmentLocalDataSource.getByOwner(type, taskId)
        val now = System.currentTimeMillis()
        inTransaction {
            attachmentLocalDataSource.softDeleteByOwner(type, taskId, now)
            existing.forEach { attachment ->
                enqueueMutation(SyncEntityType.ATTACHMENT, attachment.id, SyncOperation.DELETE, now)
            }
        }
    }

    private fun AttachmentOwner.toStorage(): Pair<OwnerType, String> = when (this) {
        is AttachmentOwner.TaskDescription -> OwnerType.TASK_DESCRIPTION to taskId.value
        is AttachmentOwner.TaskSolution -> OwnerType.TASK_SOLUTION to taskId.value
    }
}
