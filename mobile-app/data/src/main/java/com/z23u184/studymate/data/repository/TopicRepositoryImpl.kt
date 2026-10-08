package com.z23u184.studymate.data.repository

import com.z23u184.studymate.data.datasource.local.AttachmentLocalDataSource
import com.z23u184.studymate.data.datasource.local.FlashcardBestResultLocalDataSource
import com.z23u184.studymate.data.db.StudyMateDatabase
import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.SyncEntityType
import com.z23u184.studymate.data.db.entity.SyncOperation
import com.z23u184.studymate.data.datasource.local.SyncQueueLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskSolutionLocalDataSource
import com.z23u184.studymate.data.datasource.local.TopicLocalDataSource
import com.z23u184.studymate.data.db.entity.OwnerType
import com.z23u184.studymate.data.mapper.TopicEntityMapper
import com.z23u184.studymate.data.mapper.toEpochMillisValue
import com.z23u184.studymate.data.sync.queue.SyncQueuePlanner
import com.z23u184.studymate.domain.model.Topic
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.repository.TopicRepository

class TopicRepositoryImpl(
    database: StudyMateDatabase,
    private val topicLocalDataSource: TopicLocalDataSource,
    private val taskLocalDataSource: TaskLocalDataSource,
    private val taskSolutionLocalDataSource: TaskSolutionLocalDataSource,
    private val attachmentLocalDataSource: AttachmentLocalDataSource,
    private val flashcardBestResultLocalDataSource: FlashcardBestResultLocalDataSource,
    syncQueueLocalDataSource: SyncQueueLocalDataSource,
    syncQueuePlanner: SyncQueuePlanner
) : BaseQueueingRepository(database, syncQueueLocalDataSource, syncQueuePlanner), TopicRepository {

    override suspend fun getById(id: TopicId): Topic? = topicLocalDataSource.getTopic(id.value)
        ?.takeIf { !it.isDeleted }
        ?.let(TopicEntityMapper::toDomain)

    override suspend fun getAll(): List<Topic> = topicLocalDataSource.getTopics().map(TopicEntityMapper::toDomain)

    override suspend fun save(topic: Topic) {
        val existing = topicLocalDataSource.getTopic(topic.id.value)
        val operation = if (existing == null) SyncOperation.CREATE else SyncOperation.UPDATE
        inTransaction {
            topicLocalDataSource.saveTopic(TopicEntityMapper.fromDomain(topic, remoteId = existing?.remoteId, syncState = EntitySyncState.DIRTY))
            enqueueMutation(SyncEntityType.TOPIC, topic.id.value, operation, topic.updatedAt.toEpochMillisValue())
        }
    }

    override suspend fun delete(id: TopicId) {
        val topic = topicLocalDataSource.getTopic(id.value)
            ?.takeIf { !it.isDeleted }
            ?: return

        val tasks = taskLocalDataSource.getTasksByTopic(topic.id)
            .filter { !it.isDeleted }

        val now = System.currentTimeMillis()

        inTransaction {
            for (task in tasks) {
                deleteTaskCascadeInsideTransaction(task.id, now)
            }

            val bestResult = flashcardBestResultLocalDataSource.getByTopicId(topic.id)
            if (bestResult != null && !bestResult.isDeleted) {
                flashcardBestResultLocalDataSource.markDeletedByTopicId(topic.id, now)
                enqueueMutation(
                    SyncEntityType.FLASHCARD_BEST_RESULT,
                    bestResult.id,
                    SyncOperation.DELETE,
                    now,
                )
            }

            topicLocalDataSource.softDeleteTopic(topic.id, now)

            enqueueMutation(
                SyncEntityType.TOPIC,
                topic.id,
                SyncOperation.DELETE,
                now
            )
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

    override suspend fun existsByTitle(title: String): Boolean = topicLocalDataSource.existsByTitle(title)
}
