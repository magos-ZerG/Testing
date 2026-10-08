package com.z23u184.studymate.data.datasource.local

import com.z23u184.studymate.data.db.dao.SyncQueueDao
import com.z23u184.studymate.data.db.entity.SyncEntityType
import com.z23u184.studymate.data.db.entity.SyncQueueEntity
import com.z23u184.studymate.data.db.entity.SyncQueueStatus

class SyncQueueLocalDataSource(private val syncQueueDao: SyncQueueDao) {
    suspend fun enqueue(item: SyncQueueEntity): Long = syncQueueDao.enqueue(item)
    suspend fun enqueueAll(items: List<SyncQueueEntity>) = syncQueueDao.enqueueAll(items)
    suspend fun getPending(limit: Int): List<SyncQueueEntity> = syncQueueDao.getPending(limit)
    suspend fun getProcessable(nowEpochMs: Long): List<SyncQueueEntity> = syncQueueDao.getProcessable(nowEpochMs)
    suspend fun findSimilar(entityType: SyncEntityType, entityId: String): List<SyncQueueEntity> =
        syncQueueDao.findSimilarPending(entityType, entityId)
    suspend fun markProcessing(id: Long, nowEpochMs: Long) = syncQueueDao.markProcessing(id, nowEpochMs)
    suspend fun markSynced(id: Long, nowEpochMs: Long) = syncQueueDao.markSynced(id, nowEpochMs)
    suspend fun markFailed(id: Long, status: SyncQueueStatus, error: String?, attemptCount: Int, nextRetryAtEpochMs: Long?, nowEpochMs: Long) =
        syncQueueDao.markFailed(id, status, error, attemptCount, nextRetryAtEpochMs, nowEpochMs)
    suspend fun deleteByIds(ids: List<Long>) = if (ids.isNotEmpty()) syncQueueDao.deleteByIds(ids) else Unit
}
