package com.z23u184.studymate.data.repository

import com.z23u184.studymate.data.datasource.local.AttachmentLocalDataSource
import com.z23u184.studymate.data.db.StudyMateDatabase
import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.SyncEntityType
import com.z23u184.studymate.data.db.entity.SyncOperation
import com.z23u184.studymate.data.datasource.local.SyncQueueLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskSolutionLocalDataSource
import com.z23u184.studymate.data.db.entity.OwnerType
import com.z23u184.studymate.data.mapper.TaskEntityMapper
import com.z23u184.studymate.data.mapper.toEpochMillisValue
import com.z23u184.studymate.data.sync.queue.SyncQueuePlanner
import com.z23u184.studymate.domain.model.StudyTask
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.repository.TaskRepository

class TaskRepositoryImpl(
    database: StudyMateDatabase,
    private val taskLocalDataSource: TaskLocalDataSource,
    private val taskSolutionLocalDataSource: TaskSolutionLocalDataSource,
    private val attachmentLocalDataSource: AttachmentLocalDataSource,
    syncQueueLocalDataSource: SyncQueueLocalDataSource,
    syncQueuePlanner: SyncQueuePlanner
) : BaseQueueingRepository(database, syncQueueLocalDataSource, syncQueuePlanner), TaskRepository {

    override suspend fun getById(id: TaskId): StudyTask? = taskLocalDataSource.getTask(id.value)
        ?.takeIf { !it.isDeleted }
        ?.let(TaskEntityMapper::toDomain)

    override suspend fun getByTopicId(topicId: TopicId): List<StudyTask> = taskLocalDataSource.getTasksByTopic(topicId.value)
        .map(TaskEntityMapper::toDomain)

    override suspend fun save(task: StudyTask) {
        val existing = taskLocalDataSource.getTask(task.id.value)
        val operation = if (existing == null) SyncOperation.CREATE else SyncOperation.UPDATE
        inTransaction {
            taskLocalDataSource.saveTask(TaskEntityMapper.fromDomain(task, remoteId = existing?.remoteId, syncState = EntitySyncState.DIRTY))
            enqueueMutation(SyncEntityType.TASK, task.id.value, operation, task.updatedAt.toEpochMillisValue())
        }
    }

    override suspend fun delete(id: TaskId) {
        val task = taskLocalDataSource.getTask(id.value)
            ?.takeIf { !it.isDeleted }
            ?: return

        val now = System.currentTimeMillis()

        inTransaction {
            deleteTaskCascadeInsideTransaction(task.id, now)
        }
    }

    private suspend fun deleteTaskCascadeInsideTransaction(
        taskId: String,
        now: Long,
    ) {
        val solution = taskSolutionLocalDataSource.getByTaskId(taskId)

        val descriptionAttachments = attachmentLocalDataSource.getByOwner(
            OwnerType.TASK_DESCRIPTION,
            taskId
        )

        val solutionAttachments = attachmentLocalDataSource.getByOwner(
            OwnerType.TASK_SOLUTION,
            taskId
        )

        if (solution != null && !solution.isDeleted) {
            taskSolutionLocalDataSource.softDeleteByTaskId(taskId, now)

            enqueueMutation(
                SyncEntityType.TASK_SOLUTION,
                solution.id,
                SyncOperation.DELETE,
                now
            )
        }

        attachmentLocalDataSource.softDeleteByOwner(
            OwnerType.TASK_DESCRIPTION,
            taskId,
            now
        )

        for (attachment in descriptionAttachments) {
            if (!attachment.isDeleted) {
                enqueueMutation(
                    SyncEntityType.ATTACHMENT,
                    attachment.id,
                    SyncOperation.DELETE,
                    now
                )
            }
        }

        attachmentLocalDataSource.softDeleteByOwner(
            OwnerType.TASK_SOLUTION,
            taskId,
            now
        )

        for (attachment in solutionAttachments) {
            if (!attachment.isDeleted) {
                enqueueMutation(
                    SyncEntityType.ATTACHMENT,
                    attachment.id,
                    SyncOperation.DELETE,
                    now
                )
            }
        }

        taskLocalDataSource.softDeleteTask(taskId, now)

        enqueueMutation(
            SyncEntityType.TASK,
            taskId,
            SyncOperation.DELETE,
            now
        )
    }
}
