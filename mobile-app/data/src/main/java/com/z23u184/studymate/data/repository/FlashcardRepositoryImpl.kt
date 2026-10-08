package com.z23u184.studymate.data.repository

import com.z23u184.studymate.data.datasource.local.FlashcardBestResultLocalDataSource
import com.z23u184.studymate.data.datasource.local.SyncQueueLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskSolutionLocalDataSource
import com.z23u184.studymate.data.db.StudyMateDatabase
import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.SyncEntityType
import com.z23u184.studymate.data.db.entity.SyncOperation
import com.z23u184.studymate.data.mapper.FlashcardBestResultEntityMapper
import com.z23u184.studymate.data.mapper.toEpochMillisValue
import com.z23u184.studymate.data.sync.queue.SyncQueuePlanner
import com.z23u184.studymate.domain.model.Flashcard
import com.z23u184.studymate.domain.model.FlashcardBestResult
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.repository.FlashcardRepository
import com.z23u184.studymate.domain.service.FlashcardBestResultPolicy

class FlashcardRepositoryImpl(
    database: StudyMateDatabase,
    private val taskLocalDataSource: TaskLocalDataSource,
    private val taskSolutionLocalDataSource: TaskSolutionLocalDataSource,
    private val flashcardBestResultLocalDataSource: FlashcardBestResultLocalDataSource,
    private val policy: FlashcardBestResultPolicy,
    syncQueueLocalDataSource: SyncQueueLocalDataSource,
    syncQueuePlanner: SyncQueuePlanner,
) : BaseQueueingRepository(database, syncQueueLocalDataSource, syncQueuePlanner), FlashcardRepository {

    override suspend fun getCardsByTopic(topicId: TopicId): List<Flashcard> = taskLocalDataSource
        .getTasksByTopic(topicId.value)
        .mapNotNull { task ->
            val solution = taskSolutionLocalDataSource.getByTaskId(task.id)
                ?.takeIf { !it.isDeleted && it.content.isNotBlank() }
                ?: return@mapNotNull null
            val question = buildString {
                append(task.title)
                if (task.description.isNotBlank()) {
                    append("\n\n")
                    append(task.description)
                }
            }
            Flashcard(
                taskId = TaskId(task.id),
                question = question,
                answer = solution.content,
            )
        }

    override suspend fun getBestResult(topicId: TopicId): FlashcardBestResult? = flashcardBestResultLocalDataSource
        .getByTopicId(topicId.value)
        ?.takeIf { !it.isDeleted }
        ?.let(FlashcardBestResultEntityMapper::toDomain)

    override suspend fun saveBestResultIfBetter(result: FlashcardBestResult): FlashcardBestResult {
        val existing = flashcardBestResultLocalDataSource.getByTopicId(result.topicId.value)
        val current = existing?.takeIf { !it.isDeleted }?.let(FlashcardBestResultEntityMapper::toDomain)
        if (!policy.isBetter(result, current)) return current!!

        val entity = FlashcardBestResultEntityMapper.fromDomain(
            result,
            remoteId = existing?.remoteId,
            syncState = EntitySyncState.DIRTY,
        )
        val operation = if (existing == null) SyncOperation.CREATE else SyncOperation.UPDATE
        inTransaction {
            flashcardBestResultLocalDataSource.save(entity)
            enqueueMutation(
                SyncEntityType.FLASHCARD_BEST_RESULT,
                entity.id,
                operation,
                result.updatedAt.toEpochMillisValue(),
            )
        }
        return result
    }
}
