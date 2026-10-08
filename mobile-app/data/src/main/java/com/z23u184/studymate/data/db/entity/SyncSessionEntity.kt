package com.z23u184.studymate.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_sessions")
data class SyncSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long? = null,
    val status: SyncSessionStatus = SyncSessionStatus.RUNNING,
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val failedCount: Int = 0,
    val errorMessage: String? = null,
    val trigger: SyncTrigger
)
