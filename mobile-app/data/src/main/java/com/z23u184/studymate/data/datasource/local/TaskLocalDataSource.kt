package com.z23u184.studymate.data.datasource.local

import com.z23u184.studymate.data.db.dao.TaskDao
import com.z23u184.studymate.data.db.entity.TaskEntity

class TaskLocalDataSource(private val taskDao: TaskDao) {
    suspend fun getTask(id: String): TaskEntity? = taskDao.getById(id)
    suspend fun getTasksByTopic(topicId: String): List<TaskEntity> = taskDao.getByTopicId(topicId)
    suspend fun saveTask(entity: TaskEntity) = taskDao.insertOrReplace(entity)
    suspend fun softDeleteTask(id: String, updatedAtEpochMs: Long) = taskDao.markDeleted(id, updatedAtEpochMs)
}
