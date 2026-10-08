package com.z23u184.studymate.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcard_best_results",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.NO_ACTION,
        )
    ],
    indices = [Index("topicId")]
)
data class FlashcardBestResultEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val questionsCount: Int,
    val durationMs: Long,
    val completedAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val remoteId: String? = null,
    val isDeleted: Boolean = false,
    val syncState: EntitySyncState = EntitySyncState.DIRTY,
)
