package com.z23u184.studymate.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.z23u184.studymate.data.db.entity.SyncSessionEntity
import com.z23u184.studymate.data.db.entity.SyncSessionStatus

@Dao
interface SyncSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SyncSessionEntity): Long

    @Query("UPDATE sync_sessions SET finishedAtEpochMs = :finishedAtEpochMs, status = :status, pushedCount = :pushedCount, pulledCount = :pulledCount, failedCount = :failedCount, errorMessage = :errorMessage WHERE id = :id")
    suspend fun finishSession(
        id: Long,
        finishedAtEpochMs: Long,
        status: SyncSessionStatus,
        pushedCount: Int,
        pulledCount: Int,
        failedCount: Int,
        errorMessage: String?
    )

    @Query("SELECT * FROM sync_sessions WHERE status = 'SUCCESS' ORDER BY finishedAtEpochMs DESC LIMIT 1")
    suspend fun getLatestSuccessfulSession(): SyncSessionEntity?

    @Query("DELETE FROM sync_sessions")
    suspend fun deleteAll()
}
