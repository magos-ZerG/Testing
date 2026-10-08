package com.z23u184.studymate.data.sync.queue

import com.z23u184.studymate.data.db.entity.SyncOperation
import com.z23u184.studymate.data.db.entity.SyncQueueEntity

data class QueueMergeDecision(
    val keepExistingIds: List<Long>,
    val enqueueOperation: SyncOperation?,
    val dropIncoming: Boolean = false
)

class QueueMutationMerger {
    fun merge(existing: List<SyncQueueEntity>, incomingOperation: SyncOperation): QueueMergeDecision {
        if (existing.isEmpty()) {
            return QueueMergeDecision(emptyList(), incomingOperation)
        }

        val latest = existing.maxByOrNull { it.createdAtEpochMs } ?: return QueueMergeDecision(emptyList(), incomingOperation)
        return when (latest.operation) {
            SyncOperation.CREATE -> when (incomingOperation) {
                SyncOperation.CREATE -> QueueMergeDecision(existing.map { it.id }, SyncOperation.CREATE)
                SyncOperation.UPDATE -> QueueMergeDecision(existing.map { it.id }, SyncOperation.CREATE)
                SyncOperation.DELETE -> QueueMergeDecision(emptyList(), null)
            }
            SyncOperation.UPDATE -> when (incomingOperation) {
                SyncOperation.CREATE -> QueueMergeDecision(existing.map { it.id }, SyncOperation.UPDATE)
                SyncOperation.UPDATE -> QueueMergeDecision(existing.map { it.id }, SyncOperation.UPDATE)
                SyncOperation.DELETE -> QueueMergeDecision(existing.map { it.id }, SyncOperation.DELETE)
            }
            SyncOperation.DELETE -> when (incomingOperation) {
                SyncOperation.CREATE -> QueueMergeDecision(existing.map { it.id }, SyncOperation.CREATE)
                SyncOperation.UPDATE -> QueueMergeDecision(existing.map { it.id }, SyncOperation.DELETE, dropIncoming = true)
                SyncOperation.DELETE -> QueueMergeDecision(existing.map { it.id }, SyncOperation.DELETE)
            }
        }
    }
}
