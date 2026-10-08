package com.z23u184.studymate.data.datasource.local

import com.z23u184.studymate.data.db.dao.TopicDao
import com.z23u184.studymate.data.db.entity.TopicEntity

class TopicLocalDataSource(private val topicDao: TopicDao) {
    suspend fun getTopic(id: String): TopicEntity? = topicDao.getById(id)
    suspend fun getTopics(): List<TopicEntity> = topicDao.getAllActive()
    suspend fun saveTopic(entity: TopicEntity) = topicDao.insertOrReplace(entity)
    suspend fun existsByTitle(title: String): Boolean = topicDao.existsByTitle(title)
    suspend fun softDeleteTopic(id: String, updatedAtEpochMs: Long) = topicDao.markDeleted(id, updatedAtEpochMs)
}
