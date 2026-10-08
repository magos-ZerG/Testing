package com.z23u184.studymate.data.mapper

import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.FlashcardBestResultEntity
import com.z23u184.studymate.domain.model.FlashcardBestResult
import com.z23u184.studymate.domain.model.TopicId

object FlashcardBestResultEntityMapper {
    fun toDomain(entity: FlashcardBestResultEntity): FlashcardBestResult = FlashcardBestResult(
        id = entity.id,
        topicId = TopicId(entity.topicId),
        questionsCount = entity.questionsCount,
        durationMs = entity.durationMs,
        completedAt = entity.completedAtEpochMs.toInstantValue(),
        updatedAt = entity.updatedAtEpochMs.toInstantValue(),
    )

    fun fromDomain(
        model: FlashcardBestResult,
        remoteId: String? = null,
        isDeleted: Boolean = false,
        syncState: EntitySyncState = EntitySyncState.DIRTY,
    ): FlashcardBestResultEntity = FlashcardBestResultEntity(
        id = model.id,
        topicId = model.topicId.value,
        questionsCount = model.questionsCount,
        durationMs = model.durationMs,
        completedAtEpochMs = model.completedAt.toEpochMillisValue(),
        updatedAtEpochMs = model.updatedAt.toEpochMillisValue(),
        remoteId = remoteId,
        isDeleted = isDeleted,
        syncState = syncState,
    )
}
