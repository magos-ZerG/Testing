package com.z23u184.studymate.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.z23u184.studymate.domain.model.AttachmentUploadState

@Entity(
    tableName = "attachments",
    indices = [Index("ownerType", "ownerTaskId")]
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val ownerType: OwnerType,
    val ownerTaskId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val localPath: String? = null,
    val remoteFileId: String? = null,
    val uploadState: AttachmentUploadState = AttachmentUploadState.NOT_REQUIRED,
    val remoteId: String? = null,
    val isDeleted: Boolean = false,
    val syncState: EntitySyncState = EntitySyncState.DIRTY
)
