package com.z23u184.studymate.data.repository

import com.z23u184.studymate.data.db.StudyMateDatabase
import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.SyncEntityType
import com.z23u184.studymate.data.db.entity.SyncOperation
import com.z23u184.studymate.data.datasource.local.SyncQueueLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskSolutionLocalDataSource
import com.z23u184.studymate.data.mapper.TaskSolutionEntityMapper
import com.z23u184.studymate.data.mapper.toEpochMillisValue
import com.z23u184.studymate.data.sync.queue.SyncQueuePlanner
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TaskSolution
import com.z23u184.studymate.domain.repository.TaskSolutionRepository

class TaskSolutionRepositoryImpl(
    database: StudyMateDatabase,
    private val taskSolutionLocalDataSource: TaskSolutionLocalDataSource,
    syncQueueLocalDataSource: SyncQueueLocalDataSource,
    syncQueuePlanner: SyncQueuePlanner
) : BaseQueueingRepository(database, syncQueueLocalDataSource, syncQueuePlanner), TaskSolutionRepository {

    override suspend fun getByTaskId(taskId: TaskId): TaskSolution? = taskSolutionLocalDataSource.getByTaskId(taskId.value)
        ?.takeIf { !it.isDeleted }
        ?.let(TaskSolutionEntityMapper::toDomain)

    override suspend fun save(solution: TaskSolution) {
        val existing = taskSolutionLocalDataSource.getByTaskId(solution.taskId.value)
        val operation = if (existing == null) SyncOperation.CREATE else SyncOperation.UPDATE
        inTransaction {
            taskSolutionLocalDataSource.save(
                TaskSolutionEntityMapper.fromDomain(
                    solution,
                    remoteId = existing?.remoteId,
                    syncState = EntitySyncState.DIRTY,
                )
            )
            enqueueMutation(SyncEntityType.TASK_SOLUTION, solution.id.value, operation, solution.updatedAt.toEpochMillisValue())
        }
    }

    override suspend fun deleteByTaskId(taskId: TaskId) {
        val existing = taskSolutionLocalDataSource.getByTaskId(taskId.value) ?: return
        inTransaction {
            taskSolutionLocalDataSource.softDeleteByTaskId(taskId.value, System.currentTimeMillis())
            enqueueMutation(SyncEntityType.TASK_SOLUTION, existing.id, SyncOperation.DELETE, System.currentTimeMillis())
        }
    }
}
