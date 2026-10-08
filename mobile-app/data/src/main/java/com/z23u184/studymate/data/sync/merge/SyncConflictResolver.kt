package com.z23u184.studymate.data.sync.merge

import com.z23u184.studymate.data.db.entity.AttachmentEntity
import com.z23u184.studymate.data.db.entity.FlashcardBestResultEntity
import com.z23u184.studymate.data.db.entity.TaskEntity
import com.z23u184.studymate.data.db.entity.TaskSolutionEntity
import com.z23u184.studymate.data.db.entity.TopicEntity

enum class MergeWinner {
    LOCAL,
    REMOTE
}

class SyncConflictResolver {
    fun chooseTopic(local: TopicEntity?, remote: TopicEntity): MergeWinner = choose(local?.updatedAtEpochMs, local?.isDeleted, remote.updatedAtEpochMs, remote.isDeleted)
    fun chooseTask(local: TaskEntity?, remote: TaskEntity): MergeWinner = choose(local?.updatedAtEpochMs, local?.isDeleted, remote.updatedAtEpochMs, remote.isDeleted)
    fun chooseSolution(local: TaskSolutionEntity?, remote: TaskSolutionEntity): MergeWinner = choose(local?.updatedAtEpochMs, local?.isDeleted, remote.updatedAtEpochMs, remote.isDeleted)
    fun chooseAttachment(local: AttachmentEntity?, remote: AttachmentEntity): MergeWinner = choose(local?.updatedAtEpochMs, local?.isDeleted, remote.updatedAtEpochMs, remote.isDeleted)

    fun chooseFlashcardBestResult(local: FlashcardBestResultEntity?, remote: FlashcardBestResultEntity): MergeWinner {
        if (local == null) return MergeWinner.REMOTE
        if (remote.isDeleted && remote.updatedAtEpochMs >= local.updatedAtEpochMs) return MergeWinner.REMOTE
        if (local.isDeleted && local.updatedAtEpochMs > remote.updatedAtEpochMs) return MergeWinner.LOCAL
        if (remote.questionsCount != local.questionsCount) {
            return if (remote.questionsCount > local.questionsCount) MergeWinner.REMOTE else MergeWinner.LOCAL
        }
        if (remote.durationMs != local.durationMs) {
            return if (remote.durationMs < local.durationMs) MergeWinner.REMOTE else MergeWinner.LOCAL
        }
        return if (remote.updatedAtEpochMs >= local.updatedAtEpochMs) MergeWinner.REMOTE else MergeWinner.LOCAL
    }

    private fun choose(localUpdatedAt: Long?, localDeleted: Boolean?, remoteUpdatedAt: Long, remoteDeleted: Boolean): MergeWinner {
        if (localUpdatedAt == null) return MergeWinner.REMOTE
        if (remoteDeleted && localDeleted != true) return MergeWinner.REMOTE
        if (localDeleted == true && !remoteDeleted) return MergeWinner.LOCAL
        return if (remoteUpdatedAt >= localUpdatedAt) MergeWinner.REMOTE else MergeWinner.LOCAL
    }
}
