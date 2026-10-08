package com.z23u184.studymate.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.z23u184.studymate.data.db.entity.SyncEntityType
import com.z23u184.studymate.data.db.entity.SyncQueueEntity
import com.z23u184.studymate.data.db.entity.SyncQueueStatus

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueAll(items: List<SyncQueueEntity>)

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAtEpochMs ASC LIMIT :limit")
    suspend fun getPending(limit: Int): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'FAILED', 'PROCESSING') AND (nextRetryAtEpochMs IS NULL OR nextRetryAtEpochMs <= :nowEpochMs) ORDER BY createdAtEpochMs ASC")
    suspend fun getProcessable(nowEpochMs: Long): List<SyncQueueEntity>

    @Query("UPDATE sync_queue SET status = 'PROCESSING', updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun markProcessing(id: Long, updatedAtEpochMs: Long)

    @Query("UPDATE sync_queue SET status = 'SYNCED', updatedAtEpochMs = :updatedAtEpochMs, lastError = NULL WHERE id = :id")
    suspend fun markSynced(id: Long, updatedAtEpochMs: Long)

    @Query("UPDATE sync_queue SET status = :status, lastError = :error, attemptCount = :attemptCount, nextRetryAtEpochMs = :nextRetryAtEpochMs, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun markFailed(id: Long, status: SyncQueueStatus, error: String?, attemptCount: Int, nextRetryAtEpochMs: Long?, updatedAtEpochMs: Long)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED' AND updatedAtEpochMs < :timeEpochMs")
    suspend fun deleteSyncedOlderThan(timeEpochMs: Long)

    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId AND status IN ('PENDING', 'FAILED', 'PROCESSING') ORDER BY createdAtEpochMs DESC")
    suspend fun findSimilarPending(entityType: SyncEntityType, entityId: String): List<SyncQueueEntity>

    @Query("DELETE FROM sync_queue WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()
}
