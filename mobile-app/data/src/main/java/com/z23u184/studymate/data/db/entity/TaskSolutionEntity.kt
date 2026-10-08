package com.z23u184.studymate.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_solutions",
    indices = [Index(value = ["taskId"], unique = true)]
)
data class TaskSolutionEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val content: String,
    val updatedAtEpochMs: Long,
    val remoteId: String? = null,
    val isDeleted: Boolean = false,
    val syncState: EntitySyncState = EntitySyncState.DIRTY
)
