package com.z23u184.studymate.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.TopicEntity

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TopicEntity?

    @Query("SELECT * FROM topics WHERE isDeleted = 0 ORDER BY updatedAtEpochMs DESC")
    suspend fun getAllActive(): List<TopicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: TopicEntity)

    @Query("SELECT COUNT(*) > 0 FROM topics WHERE isDeleted = 0 AND title = :title")
    suspend fun existsByTitle(title: String): Boolean

    @Query("UPDATE topics SET isDeleted = 1, updatedAtEpochMs = :updatedAtEpochMs, syncState = :syncState WHERE id = :id")
    suspend fun markDeleted(id: String, updatedAtEpochMs: Long, syncState: EntitySyncState = EntitySyncState.PENDING_DELETE)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteHard(id: String)

    @Query("UPDATE topics SET remoteId = :remoteId, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun updateRemoteMetadata(id: String, remoteId: String?, syncState: EntitySyncState, updatedAtEpochMs: Long)

    @Query("DELETE FROM topics")
    suspend fun deleteAll()
}
