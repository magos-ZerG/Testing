package com.z23u184.studymate.data.sync.gateway

import com.z23u184.studymate.data.db.entity.SyncTrigger
import com.z23u184.studymate.data.sync.StudyDataSyncCoordinator
import com.z23u184.studymate.data.sync.worker.SyncWorkScheduler
import com.z23u184.studymate.domain.logging.StudyMateLogger
import com.z23u184.studymate.domain.service.SyncGateway

class SyncGatewayImpl(
    private val syncWorkScheduler: SyncWorkScheduler,
    private val syncCoordinator: StudyDataSyncCoordinator,
) : SyncGateway {
    override suspend fun requestSync() {
        StudyMateLogger.d(TAG, "Background sync requested")
        syncWorkScheduler.requestSync(SyncTrigger.AFTER_MUTATION)
    }

    override suspend fun requestLoginSync() {
        StudyMateLogger.i(TAG, "Login sync requested")
        syncCoordinator.syncNow(SyncTrigger.LOGIN)
    }

    override suspend fun requestLogoutSync() {
        StudyMateLogger.i(TAG, "Logout sync requested")
        val result = syncCoordinator.syncNow(SyncTrigger.MANUAL)
        check(result.isSuccess) { "Не удалось завершить синхронизацию перед выходом" }
    }

    private companion object {
        const val TAG = "SyncGateway"
    }
}
