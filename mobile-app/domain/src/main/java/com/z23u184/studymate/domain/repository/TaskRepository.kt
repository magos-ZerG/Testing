package com.z23u184.studymate.domain.repository

import com.z23u184.studymate.domain.model.StudyTask
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TopicId

interface TaskRepository {
    suspend fun getById(id: TaskId): StudyTask?
    suspend fun getByTopicId(topicId: TopicId): List<StudyTask>
    suspend fun save(task: StudyTask)
    suspend fun delete(id: TaskId)
}
