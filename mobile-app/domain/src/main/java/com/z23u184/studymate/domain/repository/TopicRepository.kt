package com.z23u184.studymate.domain.repository

import com.z23u184.studymate.domain.model.Topic
import com.z23u184.studymate.domain.model.TopicId

interface TopicRepository {
    suspend fun getById(id: TopicId): Topic?
    suspend fun getAll(): List<Topic>
    suspend fun save(topic: Topic)
    suspend fun delete(id: TopicId)
    suspend fun existsByTitle(title: String): Boolean
}
