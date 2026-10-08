package com.z23u184.studymate.data.sync.worker

import com.z23u184.studymate.data.db.entity.SyncTrigger

interface SyncWorkScheduler {
    suspend fun requestSync(trigger: SyncTrigger = SyncTrigger.AFTER_MUTATION)
}

class ImmediateSyncWorkScheduler(
    private val syncBlock: suspend (SyncTrigger) -> Unit
) : SyncWorkScheduler {
    override suspend fun requestSync(trigger: SyncTrigger) {
        syncBlock(trigger)
    }
}
