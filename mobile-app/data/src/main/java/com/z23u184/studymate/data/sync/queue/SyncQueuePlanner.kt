package com.z23u184.studymate.data.sync.queue

import com.z23u184.studymate.data.db.entity.SyncEntityType
import com.z23u184.studymate.data.db.entity.SyncOperation
import com.z23u184.studymate.data.db.entity.SyncQueueEntity
import com.z23u184.studymate.data.db.entity.SyncQueueStatus

class SyncQueuePlanner(
    private val queueMutationMerger: QueueMutationMerger
) {
    fun plan(existing: List<SyncQueueEntity>, entityType: SyncEntityType, entityId: String, operation: SyncOperation, nowEpochMs: Long): List<SyncQueueEntity> {
        val decision = queueMutationMerger.merge(existing, operation)
        if (decision.dropIncoming && decision.enqueueOperation == null) {
            return emptyList()
        }
        return decision.enqueueOperation?.let {
            listOf(
                SyncQueueEntity(
                    entityType = entityType,
                    entityId = entityId,
                    operation = it,
                    status = SyncQueueStatus.PENDING,
                    createdAtEpochMs = nowEpochMs,
                    updatedAtEpochMs = nowEpochMs,
                    deduplicationKey = "$entityType:$entityId"
                )
            )
        } ?: emptyList()
    }
}
