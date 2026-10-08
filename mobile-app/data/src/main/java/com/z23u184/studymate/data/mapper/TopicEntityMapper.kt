package com.z23u184.studymate.data.mapper

import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.TopicEntity
import com.z23u184.studymate.domain.model.Topic
import com.z23u184.studymate.domain.model.TopicId

object TopicEntityMapper {
    fun toDomain(entity: TopicEntity): Topic = Topic(
        id = TopicId(entity.id),
        title = entity.title,
        createdAt = entity.createdAtEpochMs.toInstantValue(),
        updatedAt = entity.updatedAtEpochMs.toInstantValue()
    )

    fun fromDomain(
        model: Topic,
        remoteId: String? = null,
        isDeleted: Boolean = false,
        syncState: EntitySyncState = EntitySyncState.DIRTY
    ): TopicEntity = TopicEntity(
        id = model.id.value,
        title = model.title,
        createdAtEpochMs = model.createdAt.toEpochMillisValue(),
        updatedAtEpochMs = model.updatedAt.toEpochMillisValue(),
        remoteId = remoteId,
        isDeleted = isDeleted,
        syncState = syncState
    )
}
