package com.z23u184.studymate.data.session

import androidx.room.withTransaction
import com.z23u184.studymate.data.db.StudyMateDatabase
import com.z23u184.studymate.data.file.AttachmentFileManager
import com.z23u184.studymate.domain.model.AttachmentUploadState

class LocalStudyDataCleaner(
    private val database: StudyMateDatabase,
    private val attachmentFileManager: AttachmentFileManager,
) {
    suspend fun clearAccountStudyData() {
        val attachments = database.attachmentDao().getAllForCleanup()

        attachments.forEach { attachment ->
            val canDeleteLocalFile =
                attachment.localPath != null &&
                        (
                                attachment.uploadState == AttachmentUploadState.UPLOADED ||
                                        attachment.remoteFileId != null ||
                                        attachment.remoteId != null
                                )

            if (canDeleteLocalFile) {
                attachmentFileManager.deleteIfExists(attachment.localPath)
            }
        }

        database.withTransaction {
            database.syncQueueDao().deleteAll()
            database.syncSessionDao().deleteAll()
            database.attachmentDao().deleteAll()
            database.flashcardBestResultDao().deleteAll()
            database.taskSolutionDao().deleteAll()
            database.taskDao().deleteAll()
            database.topicDao().deleteAll()
        }
    }
}