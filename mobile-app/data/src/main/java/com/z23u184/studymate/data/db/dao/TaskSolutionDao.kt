package com.z23u184.studymate.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.TaskSolutionEntity

@Dao
interface TaskSolutionDao {
    @Query("SELECT * FROM task_solutions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskSolutionEntity?

    @Query("SELECT * FROM task_solutions WHERE taskId = :taskId LIMIT 1")
    suspend fun getByTaskId(taskId: String): TaskSolutionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: TaskSolutionEntity)

    @Query("UPDATE task_solutions SET isDeleted = 1, updatedAtEpochMs = :updatedAtEpochMs, syncState = :syncState WHERE taskId = :taskId")
    suspend fun markDeletedByTaskId(taskId: String, updatedAtEpochMs: Long, syncState: EntitySyncState = EntitySyncState.PENDING_DELETE)

    @Query("DELETE FROM task_solutions WHERE taskId = :taskId")
    suspend fun deleteHardByTaskId(taskId: String)

    @Query("UPDATE task_solutions SET remoteId = :remoteId, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun updateRemoteMetadata(id: String, remoteId: String?, syncState: EntitySyncState, updatedAtEpochMs: Long)

    @Query("DELETE FROM task_solutions")
    suspend fun deleteAll()
}
