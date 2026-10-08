package com.z23u184.studymate.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class Attachment(
    val id: AttachmentId,
    val owner: AttachmentOwner,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Instant,
    val updatedAt: Instant = createdAt,
    val localPath: String? = null,
    val remoteId: String? = null,
    val remoteFileId: String? = null,
    val uploadState: AttachmentUploadState = AttachmentUploadState.NOT_REQUIRED
)
