package com.z23u184.studymate.data.sync.remote

import com.z23u184.studymate.data.network.api.StudyMateSyncApi
import com.z23u184.studymate.data.network.dto.InitialSyncRequestDto
import com.z23u184.studymate.data.network.dto.InitialSyncResponseDto
import com.z23u184.studymate.data.network.dto.PullChangesResponseDto
import com.z23u184.studymate.data.network.dto.PushChangesRequestDto
import com.z23u184.studymate.data.network.dto.PushChangesResponseDto
import java.io.File

interface SyncRemoteDataSource {
    suspend fun pushChanges(request: PushChangesRequestDto): PushChangesResponseDto
    suspend fun pullChanges(sinceIsoString: String?): PullChangesResponseDto
    suspend fun initialSync(request: InitialSyncRequestDto): InitialSyncResponseDto
    suspend fun uploadAttachment(attachmentId: String, file: File, mimeType: String?): StudyMateSyncApi.UploadAttachmentResponseDto
}
