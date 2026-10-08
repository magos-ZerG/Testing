package com.z23u184.studymate.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.FlashcardBestResultEntity

@Dao
interface FlashcardBestResultDao {
    @Query("SELECT * FROM flashcard_best_results WHERE topicId = :topicId AND isDeleted = 0 LIMIT 1")
    suspend fun getByTopicId(topicId: String): FlashcardBestResultEntity?

    @Query("SELECT * FROM flashcard_best_results WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FlashcardBestResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: FlashcardBestResultEntity)

    @Query("UPDATE flashcard_best_results SET remoteId = :remoteId, syncState = :syncState, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :id")
    suspend fun updateRemoteMetadata(id: String, remoteId: String?, syncState: EntitySyncState, updatedAtEpochMs: Long)

    @Query("UPDATE flashcard_best_results SET isDeleted = 1, updatedAtEpochMs = :updatedAtEpochMs, syncState = :syncState WHERE topicId = :topicId")
    suspend fun markDeletedByTopicId(topicId: String, updatedAtEpochMs: Long, syncState: EntitySyncState = EntitySyncState.PENDING_DELETE)

    @Query("DELETE FROM flashcard_best_results")
    suspend fun deleteAll()
}
