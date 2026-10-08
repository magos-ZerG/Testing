package com.z23u184.studymate.data.repository

import androidx.room.withTransaction
import com.z23u184.studymate.data.db.StudyMateDatabase
import com.z23u184.studymate.data.db.entity.SyncEntityType
import com.z23u184.studymate.data.db.entity.SyncOperation
import com.z23u184.studymate.data.datasource.local.SyncQueueLocalDataSource
import com.z23u184.studymate.data.sync.queue.SyncQueuePlanner
import com.z23u184.studymate.domain.logging.StudyMateLogger

abstract class BaseQueueingRepository(
    private val database: StudyMateDatabase,
    private val syncQueueLocalDataSource: SyncQueueLocalDataSource,
    private val syncQueuePlanner: SyncQueuePlanner
) {
    protected suspend fun inTransaction(block: suspend () -> Unit) {
        database.withTransaction { block() }
    }

    protected suspend fun enqueueMutation(entityType: SyncEntityType, entityId: String, operation: SyncOperation, nowEpochMs: Long) {
        val existing = syncQueueLocalDataSource.findSimilar(entityType, entityId)
        val planned = syncQueuePlanner.plan(existing, entityType, entityId, operation, nowEpochMs)
        StudyMateLogger.d(
            TAG,
            "Queue mutation entityType=$entityType entityId=$entityId operation=$operation existing=${existing.size} planned=${planned.size}"
        )
        syncQueueLocalDataSource.deleteByIds(existing.map { it.id })
        syncQueueLocalDataSource.enqueueAll(planned)
    }

    private companion object {
        const val TAG = "BaseQueueingRepository"
    }
}
