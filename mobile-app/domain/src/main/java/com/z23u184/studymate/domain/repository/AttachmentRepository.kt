package com.z23u184.studymate.domain.repository

import com.z23u184.studymate.domain.model.Attachment
import com.z23u184.studymate.domain.model.AttachmentId
import com.z23u184.studymate.domain.model.AttachmentOwner

interface AttachmentRepository {
    suspend fun getById(id: AttachmentId): Attachment?
    suspend fun getByOwner(owner: AttachmentOwner): List<Attachment>
    suspend fun save(attachment: Attachment)
    suspend fun delete(id: AttachmentId)
    suspend fun deleteByOwner(owner: AttachmentOwner)
}
