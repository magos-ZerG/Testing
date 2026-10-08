package com.z23u184.studymate.data.sync.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.z23u184.studymate.data.db.entity.SyncTrigger
import com.z23u184.studymate.domain.logging.StudyMateLogger
import java.util.concurrent.TimeUnit

class WorkManagerSyncWorkScheduler(
    context: Context,
) : SyncWorkScheduler {

    private val workManager = WorkManager.getInstance(context)

    override suspend fun requestSync(trigger: SyncTrigger) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(
                Data.Builder()
                    .putString(SyncWorker.KEY_TRIGGER, trigger.name)
                    .build(),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS,
            )
            .build()

        StudyMateLogger.i(TAG, "Enqueue sync work trigger=$trigger workId=${request.id}")
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "studymate_sync"
        const val TAG = "WorkManagerSyncScheduler"
    }
}
