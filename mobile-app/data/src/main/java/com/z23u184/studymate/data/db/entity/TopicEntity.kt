package com.z23u184.studymate.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val remoteId: String? = null,
    val isDeleted: Boolean = false,
    val syncState: EntitySyncState = EntitySyncState.DIRTY
)
