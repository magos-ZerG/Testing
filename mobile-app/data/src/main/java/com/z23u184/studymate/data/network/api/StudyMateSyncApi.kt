package com.z23u184.studymate.data.network.api

import com.z23u184.studymate.data.network.dto.InitialSyncRequestDto
import com.z23u184.studymate.data.network.dto.InitialSyncResponseDto
import com.z23u184.studymate.data.network.dto.PullChangesResponseDto
import com.z23u184.studymate.data.network.dto.PushChangesRequestDto
import com.z23u184.studymate.data.network.dto.PushChangesResponseDto
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface StudyMateSyncApi {
    @POST("/api/v1/sync/push")
    suspend fun pushChanges(
        @Body request: PushChangesRequestDto,
    ): PushChangesResponseDto

    @GET("/api/v1/sync/pull")
    suspend fun pullChanges(
        @Query("since") sinceIsoString: String?,
    ): PullChangesResponseDto

    @POST("/api/v1/sync/initial")
    suspend fun initialSync(
        @Body request: InitialSyncRequestDto,
    ): InitialSyncResponseDto

    @Multipart
    @POST("/api/v1/attachments/{attachmentId}/upload")
    suspend fun uploadAttachment(
        @Path("attachmentId") attachmentId: String,
        @Part file: MultipartBody.Part,
    ): UploadAttachmentResponseDto

    @Streaming
    @GET("/api/v1/attachments/{attachmentId}/download")
    suspend fun downloadAttachment(
        @Path("attachmentId") attachmentId: String,
    ): ResponseBody

    @Serializable
    data class UploadAttachmentResponseDto(
        val id: String,
        val remoteFileId: String?,
        val storageKey: String?,
        val sizeBytes: Long?,
        val uploadState: String?,
        val updatedAt: String,
    )
}
