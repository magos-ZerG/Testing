package com.z23u184.studymate.data.file

import com.z23u184.studymate.data.datasource.local.AttachmentLocalDataSource
import com.z23u184.studymate.data.network.api.StudyMateSyncApi
import java.io.File

class AttachmentRemoteFileDownloader(
    private val api: StudyMateSyncApi,
    private val attachmentFileManager: AttachmentFileManager,
    private val attachmentLocalDataSource: AttachmentLocalDataSource,
) {
    suspend fun download(
        remoteAttachmentId: String,
        fileName: String,
        localAttachmentId: String? = null,
    ): File {
        val bytes = api.downloadAttachment(remoteAttachmentId).bytes()
        val file = attachmentFileManager.saveDownloadedBytes(fileName, bytes)
        if (!localAttachmentId.isNullOrBlank()) {
            attachmentLocalDataSource.updateLocalPath(localAttachmentId, file.absolutePath)
        }
        return file
    }
}
