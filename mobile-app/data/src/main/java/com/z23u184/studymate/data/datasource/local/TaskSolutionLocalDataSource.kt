package com.z23u184.studymate.data.datasource.local

import com.z23u184.studymate.data.db.dao.TaskSolutionDao
import com.z23u184.studymate.data.db.entity.TaskSolutionEntity

class TaskSolutionLocalDataSource(private val taskSolutionDao: TaskSolutionDao) {
    suspend fun getById(id: String): TaskSolutionEntity? = taskSolutionDao.getById(id)
    suspend fun getByTaskId(taskId: String): TaskSolutionEntity? = taskSolutionDao.getByTaskId(taskId)
    suspend fun save(entity: TaskSolutionEntity) = taskSolutionDao.insertOrReplace(entity)
    suspend fun softDeleteByTaskId(taskId: String, updatedAtEpochMs: Long) =
        taskSolutionDao.markDeletedByTaskId(taskId, updatedAtEpochMs)
}
