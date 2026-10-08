package com.z23u184.studymate.domain.repository

import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TaskSolution

interface TaskSolutionRepository {
    suspend fun getByTaskId(taskId: TaskId): TaskSolution?
    suspend fun save(solution: TaskSolution)
    suspend fun deleteByTaskId(taskId: TaskId)
}
