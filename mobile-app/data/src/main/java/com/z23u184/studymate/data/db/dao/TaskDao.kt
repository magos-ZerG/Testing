package com.z23u184.studymate.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.TaskEntity

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE topicId = :topicId AND isDeleted = 0 ORDER BY updatedAtEpochMs DESC")
    suspend fun getByTopicId(topicId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: TaskEntity)

    @Query("UPDATE tasks SET isDeleted = 1, updatedAtEpochMs = :updatedAtEpochMs, syncState = :syncState WHERE id = :id")
    suspend fun markDeleted(id: String, updatedAtEpochMs: Long, syncState: EntitySyncState = EntitySyncState.PENDING_DELETE)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteHard(id: String)

    @Query("UPDATE tasks SET remoteId = :remoteId, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun updateRemoteMetadata(id: String, remoteId: String?, syncState: EntitySyncState, updatedAtEpochMs: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
