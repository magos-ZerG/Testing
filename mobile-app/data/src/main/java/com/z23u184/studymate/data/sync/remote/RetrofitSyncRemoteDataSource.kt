package com.z23u184.studymate.data.sync.remote

import com.z23u184.studymate.data.network.api.StudyMateSyncApi
import com.z23u184.studymate.data.network.dto.InitialSyncRequestDto
import com.z23u184.studymate.data.network.dto.InitialSyncResponseDto
import com.z23u184.studymate.data.network.dto.PullChangesResponseDto
import com.z23u184.studymate.data.network.dto.PushChangesRequestDto
import com.z23u184.studymate.data.network.dto.PushChangesResponseDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class RetrofitSyncRemoteDataSource(
    private val api: StudyMateSyncApi,
) : SyncRemoteDataSource {

    override suspend fun pushChanges(request: PushChangesRequestDto): PushChangesResponseDto {
        return api.pushChanges(request)
    }

    override suspend fun pullChanges(sinceIsoString: String?): PullChangesResponseDto {
        return api.pullChanges(sinceIsoString)
    }

    override suspend fun initialSync(request: InitialSyncRequestDto): InitialSyncResponseDto {
        return api.initialSync(request)
    }

    override suspend fun uploadAttachment(
        attachmentId: String,
        file: File,
        mimeType: String?,
    ): StudyMateSyncApi.UploadAttachmentResponseDto {
        val requestBody = file.asRequestBody(mimeType?.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData(
            name = "file",
            filename = file.name,
            body = requestBody,
        )
        return api.uploadAttachment(attachmentId, part)
    }
}
