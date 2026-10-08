package com.z23u184.studymate.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_queue",
    indices = [Index("entityType", "entityId", unique = false), Index("status"), Index("nextRetryAtEpochMs")]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: SyncEntityType,
    val entityId: String,
    val operation: SyncOperation,
    val status: SyncQueueStatus = SyncQueueStatus.PENDING,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val nextRetryAtEpochMs: Long? = null,
    val deduplicationKey: String? = null,
    val payloadSnapshotJson: String? = null
)
