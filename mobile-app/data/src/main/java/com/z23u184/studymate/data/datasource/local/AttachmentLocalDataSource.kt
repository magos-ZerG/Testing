package com.z23u184.studymate.data.datasource.local

import com.z23u184.studymate.data.db.dao.AttachmentDao
import com.z23u184.studymate.data.db.entity.AttachmentEntity
import com.z23u184.studymate.data.db.entity.OwnerType
import com.z23u184.studymate.domain.model.AttachmentUploadState

class AttachmentLocalDataSource(private val attachmentDao: AttachmentDao) {
    suspend fun getById(id: String): AttachmentEntity? = attachmentDao.getById(id)
    suspend fun getByOwner(ownerType: OwnerType, ownerTaskId: String): List<AttachmentEntity> = attachmentDao.getByOwner(ownerType, ownerTaskId)
    suspend fun save(entity: AttachmentEntity) = attachmentDao.insertOrReplace(entity)
    suspend fun softDelete(id: String, updatedAtEpochMs: Long) = attachmentDao.markDeleted(id, updatedAtEpochMs)
    suspend fun softDeleteByOwner(ownerType: OwnerType, ownerTaskId: String, updatedAtEpochMs: Long) =
        attachmentDao.markDeletedByOwner(ownerType, ownerTaskId, updatedAtEpochMs)
    suspend fun updateUploadState(id: String, uploadState: AttachmentUploadState, updatedAtEpochMs: Long) =
        attachmentDao.updateUploadState(id, uploadState, updatedAtEpochMs)

    suspend fun updateLocalPath(id: String, localPath: String?) =
        attachmentDao.updateLocalPath(id, localPath)

    suspend fun getPendingFileUploads(): List<AttachmentEntity> =
        attachmentDao.getPendingFileUploads()
}
