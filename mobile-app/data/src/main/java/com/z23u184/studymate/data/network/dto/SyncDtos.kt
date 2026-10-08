package com.z23u184.studymate.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PushChangesRequestDto(
    val topics: List<RemoteTopicPushDto> = emptyList(),
    val tasks: List<RemoteTaskPushDto> = emptyList(),
    val solutions: List<RemoteTaskSolutionPushDto> = emptyList(),
    val attachmentsMetadata: List<RemoteAttachmentPushDto> = emptyList(),
    val flashcardBestResults: List<RemoteFlashcardBestResultPushDto> = emptyList(),
)

@Serializable
sealed interface PushOperationCarrier {
    val clientId: String
    val updatedAt: String
    val isDeleted: Boolean
    val operation: String
}

@Serializable
data class RemoteTopicPushDto(
    override val clientId: String,
    override val updatedAt: String,
    override val isDeleted: Boolean = false,
    override val operation: String = "UPDATE",
    val title: String? = null,
) : PushOperationCarrier

@Serializable
data class RemoteTaskPushDto(
    override val clientId: String,
    override val updatedAt: String,
    override val isDeleted: Boolean = false,
    override val operation: String = "UPDATE",
    val topicClientId: String? = null,
    val topicRemoteId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val status: String = "PLANNED",
    val deadlineAt: String? = null,
) : PushOperationCarrier

@Serializable
data class RemoteTaskSolutionPushDto(
    override val clientId: String,
    override val updatedAt: String,
    override val isDeleted: Boolean = false,
    override val operation: String = "UPDATE",
    val taskClientId: String? = null,
    val taskRemoteId: String? = null,
    val content: String? = null,
) : PushOperationCarrier

@Serializable
data class RemoteAttachmentPushDto(
    override val clientId: String,
    override val updatedAt: String,
    override val isDeleted: Boolean = false,
    override val operation: String = "UPDATE",
    val ownerType: String? = null,
    val ownerTaskClientId: String? = null,
    val ownerTaskRemoteId: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val remoteFileId: String? = null,
    val uploadState: String? = null,
) : PushOperationCarrier

@Serializable
data class RemoteFlashcardBestResultPushDto(
    override val clientId: String,
    override val updatedAt: String,
    override val isDeleted: Boolean = false,
    override val operation: String = "UPDATE",
    val topicClientId: String? = null,
    val topicRemoteId: String? = null,
    val questionsCount: Int? = null,
    val durationMs: Long? = null,
    val completedAt: String? = null,
) : PushOperationCarrier

@Serializable
data class PushResultItemDto(
    val clientId: String,
    val remoteId: String,
    val serverUpdatedAt: String,
    val status: String,
)

@Serializable
data class PushChangesResponseDto(
    val topics: List<PushResultItemDto> = emptyList(),
    val tasks: List<PushResultItemDto> = emptyList(),
    val solutions: List<PushResultItemDto> = emptyList(),
    val attachmentsMetadata: List<PushResultItemDto> = emptyList(),
    val flashcardBestResults: List<PushResultItemDto> = emptyList(),
)

@Serializable
data class RemoteTopicPullDto(
    val remoteId: String,
    val clientId: String,
    val title: String,
    val createdAt: String,
    val updatedAt: String,
    val isDeleted: Boolean,
)

@Serializable
data class RemoteTaskPullDto(
    val remoteId: String,
    val clientId: String,
    val topicRemoteId: String,
    val topicClientId: String,
    val title: String,
    val description: String? = null,
    val status: String,
    val deadlineAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val isDeleted: Boolean,
)

@Serializable
data class RemoteTaskSolutionPullDto(
    val remoteId: String,
    val clientId: String,
    val taskRemoteId: String,
    val taskClientId: String,
    val content: String? = null,
    val updatedAt: String,
    val isDeleted: Boolean,
)

@Serializable
data class RemoteAttachmentPullDto(
    val remoteId: String,
    val clientId: String,
    val ownerType: String,
    val ownerTaskRemoteId: String,
    val ownerTaskClientId: String,
    val fileName: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val createdAt: String,
    val updatedAt: String,
    val isDeleted: Boolean,
    val remoteFileId: String? = null,
    val storageKey: String? = null,
    val uploadState: String? = null,
)

@Serializable
data class RemoteFlashcardBestResultPullDto(
    val remoteId: String,
    val clientId: String,
    val topicRemoteId: String,
    val topicClientId: String,
    val questionsCount: Int,
    val durationMs: Long,
    val completedAt: String,
    val updatedAt: String,
    val isDeleted: Boolean,
)

@Serializable
data class PullChangesResponseDto(
    val serverTime: String,
    val topics: List<RemoteTopicPullDto> = emptyList(),
    val tasks: List<RemoteTaskPullDto> = emptyList(),
    val solutions: List<RemoteTaskSolutionPullDto> = emptyList(),
    val attachmentsMetadata: List<RemoteAttachmentPullDto> = emptyList(),
    val flashcardBestResults: List<RemoteFlashcardBestResultPullDto> = emptyList(),
    val nextSince: String,
)

@Serializable
data class InitialSyncRequestDto(
    val push: PushChangesRequestDto = PushChangesRequestDto(),
    val pullSince: String? = null,
)

@Serializable
data class InitialSyncResponseDto(
    val push: PushChangesResponseDto,
    val pull: PullChangesResponseDto,
)

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequestDto(
    val refreshToken: String,
)

@Serializable
data class TokenPairResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "bearer",
)

@Serializable
data class UserResponseDto(
    val id: String,
    val email: String,
    val createdAt: String,
)
