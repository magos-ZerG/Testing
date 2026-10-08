package com.z23u184.studymate.data.sync.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z23u184.studymate.data.db.entity.SyncTrigger
import com.z23u184.studymate.data.sync.StudyDataSyncCoordinator
import com.z23u184.studymate.domain.logging.StudyMateLogger
import org.koin.core.context.GlobalContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    private val coordinator: StudyDataSyncCoordinator
        get() = GlobalContext.get().get()

    override suspend fun doWork(): Result {
        val trigger = inputData.getString(KEY_TRIGGER)
            ?.let { value -> runCatching { SyncTrigger.valueOf(value) }.getOrNull() }
            ?: SyncTrigger.WORKER

        StudyMateLogger.i(TAG, "Worker sync started trigger=$trigger attempt=$runAttemptCount")
        val result = coordinator.syncNow(trigger = trigger)
        return if (result.shouldRetry) {
            StudyMateLogger.w(TAG, "Worker sync will retry trigger=$trigger failed=${result.failedCount}")
            Result.retry()
        } else {
            StudyMateLogger.i(TAG, "Worker sync completed trigger=$trigger pushed=${result.pushedCount} pulled=${result.pulledCount}")
            Result.success()
        }
    }

    companion object {
        const val KEY_TRIGGER = "sync_trigger"
        private const val TAG = "SyncWorker"
    }
}
