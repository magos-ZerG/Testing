package com.z23u184.studymate.data.datasource.local

import com.z23u184.studymate.data.db.dao.FlashcardBestResultDao
import com.z23u184.studymate.data.db.entity.FlashcardBestResultEntity

class FlashcardBestResultLocalDataSource(
    private val dao: FlashcardBestResultDao,
) {
    suspend fun getByTopicId(topicId: String): FlashcardBestResultEntity? = dao.getByTopicId(topicId)
    suspend fun getById(id: String): FlashcardBestResultEntity? = dao.getById(id)
    suspend fun save(entity: FlashcardBestResultEntity) = dao.insertOrReplace(entity)
    suspend fun markDeletedByTopicId(topicId: String, now: Long) = dao.markDeletedByTopicId(topicId, now)
}
