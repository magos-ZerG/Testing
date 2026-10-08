package com.z23u184.studymate.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("topicId")]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val title: String,
    val description: String,
    val status: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val deadlineAtEpochMs: Long? = null,
    val remoteId: String? = null,
    val isDeleted: Boolean = false,
    val syncState: EntitySyncState = EntitySyncState.DIRTY
)
