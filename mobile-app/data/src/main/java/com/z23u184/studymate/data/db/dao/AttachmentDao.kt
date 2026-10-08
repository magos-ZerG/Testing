package com.z23u184.studymate.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.z23u184.studymate.data.db.entity.AttachmentEntity
import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.OwnerType
import com.z23u184.studymate.domain.model.AttachmentUploadState

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE ownerType = :ownerType AND ownerTaskId = :ownerTaskId AND isDeleted = 0 ORDER BY updatedAtEpochMs DESC")
    suspend fun getByOwner(ownerType: OwnerType, ownerTaskId: String): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: AttachmentEntity)

    @Query("UPDATE attachments SET isDeleted = 1, updatedAtEpochMs = :updatedAtEpochMs, syncState = :syncState WHERE id = :id")
    suspend fun markDeleted(id: String, updatedAtEpochMs: Long, syncState: EntitySyncState = EntitySyncState.PENDING_DELETE)

    @Query("UPDATE attachments SET isDeleted = 1, updatedAtEpochMs = :updatedAtEpochMs, syncState = :syncState WHERE ownerType = :ownerType AND ownerTaskId = :ownerTaskId")
    suspend fun markDeletedByOwner(ownerType: OwnerType, ownerTaskId: String, updatedAtEpochMs: Long, syncState: EntitySyncState = EntitySyncState.PENDING_DELETE)

    @Query("UPDATE attachments SET uploadState = :uploadState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun updateUploadState(id: String, uploadState: AttachmentUploadState, updatedAtEpochMs: Long)

    @Query("UPDATE attachments SET remoteId = :remoteId, remoteFileId = :remoteFileId, uploadState = :uploadState, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun updateRemoteFileInfo(
        id: String,
        remoteId: String?,
        remoteFileId: String?,
        uploadState: AttachmentUploadState,
        syncState: EntitySyncState = EntitySyncState.SYNCED,
        updatedAtEpochMs: Long,
    )

    @Query("UPDATE attachments SET localPath = :localPath WHERE id = :id")
    suspend fun updateLocalPath(id: String, localPath: String?)

    @Query("DELETE FROM attachments")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM attachments
        WHERE isDeleted = 0
          AND syncState = :syncState
          AND remoteId IS NOT NULL
          AND localPath IS NOT NULL
          AND uploadState IN (:pendingStates)
    """)
    suspend fun getPendingFileUploads(
        syncState: EntitySyncState = EntitySyncState.SYNCED,
        pendingStates: List<AttachmentUploadState> = listOf(
            AttachmentUploadState.PENDING_UPLOAD,
            AttachmentUploadState.FAILED,
            AttachmentUploadState.UPLOADING,
        ),
    ): List<AttachmentEntity>

    @Query("SELECT * FROM attachments")
    suspend fun getAllForCleanup(): List<AttachmentEntity>
}
