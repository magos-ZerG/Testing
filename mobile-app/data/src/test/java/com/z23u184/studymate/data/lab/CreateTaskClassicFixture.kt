package com.z23u184.studymate.data.lab

import android.content.Context
import androidx.room.Room
import com.z23u184.studymate.data.datasource.local.AttachmentLocalDataSource
import com.z23u184.studymate.data.datasource.local.FlashcardBestResultLocalDataSource
import com.z23u184.studymate.data.datasource.local.SyncQueueLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskSolutionLocalDataSource
import com.z23u184.studymate.data.datasource.local.TopicLocalDataSource
import com.z23u184.studymate.data.db.StudyMateDatabase
import com.z23u184.studymate.data.repository.SessionRepositoryImpl
import com.z23u184.studymate.data.repository.TaskRepositoryImpl
import com.z23u184.studymate.data.repository.TopicRepositoryImpl
import com.z23u184.studymate.data.session.SessionLocalDataSource
import com.z23u184.studymate.data.sync.queue.QueueMutationMerger
import com.z23u184.studymate.data.sync.queue.SyncQueuePlanner
import com.z23u184.studymate.domain.model.AttachmentId
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TaskSolutionId
import com.z23u184.studymate.domain.model.Topic
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.model.UserMode
import com.z23u184.studymate.domain.service.ClockProvider
import com.z23u184.studymate.domain.service.DefaultSyncPolicyService
import com.z23u184.studymate.domain.service.IdGenerator
import com.z23u184.studymate.domain.service.SyncGateway
import com.z23u184.studymate.domain.usecase.CreateTaskUseCase
import com.z23u184.studymate.domain.usecase.SyncAfterMutationUseCase
import com.z23u184.studymate.domain.usecase.SyncStudyDataUseCase
import com.z23u184.studymate.domain.validation.DefaultTaskValidator
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

internal class CreateTaskClassicFixture private constructor(
    val database: StudyMateDatabase,
    val topicRepository: TopicRepositoryImpl,
    val taskRepository: TaskRepositoryImpl,
    val useCase: CreateTaskUseCase,
) : AutoCloseable {

    suspend fun seedTopic(topic: Topic = TopicMother.existing()) {
        topicRepository.save(topic)
    }

    override fun close() {
        database.close()
    }

    companion object {
        fun create(context: Context): CreateTaskClassicFixture {
            val database = Room.inMemoryDatabaseBuilder(
                context,
                StudyMateDatabase::class.java,
            )
                .allowMainThreadQueries()
                .build()

            val topicLocal = TopicLocalDataSource(database.topicDao())
            val taskLocal = TaskLocalDataSource(database.taskDao())
            val solutionLocal = TaskSolutionLocalDataSource(database.taskSolutionDao())
            val attachmentLocal = AttachmentLocalDataSource(database.attachmentDao())
            val flashcardLocal = FlashcardBestResultLocalDataSource(database.flashcardBestResultDao())
            val syncQueueLocal = SyncQueueLocalDataSource(database.syncQueueDao())
            val queuePlanner = SyncQueuePlanner(QueueMutationMerger())

            val topicRepository = TopicRepositoryImpl(
                database = database,
                topicLocalDataSource = topicLocal,
                taskLocalDataSource = taskLocal,
                taskSolutionLocalDataSource = solutionLocal,
                attachmentLocalDataSource = attachmentLocal,
                flashcardBestResultLocalDataSource = flashcardLocal,
                syncQueueLocalDataSource = syncQueueLocal,
                syncQueuePlanner = queuePlanner,
            )

            val taskRepository = TaskRepositoryImpl(
                database = database,
                taskLocalDataSource = taskLocal,
                taskSolutionLocalDataSource = solutionLocal,
                attachmentLocalDataSource = attachmentLocal,
                syncQueueLocalDataSource = syncQueueLocal,
                syncQueuePlanner = queuePlanner,
            )

            val sessionRepository = SessionRepositoryImpl(InMemorySessionLocalDataSource())

            val useCase = CreateTaskUseCase(
                topicRepository = topicRepository,
                taskRepository = taskRepository,
                taskValidator = DefaultTaskValidator(),
                idGenerator = RandomIdGenerator(),
                clockProvider = SystemClockProvider(),
                syncAfterMutationUseCase = SyncAfterMutationUseCase(
                    sessionRepository = sessionRepository,
                    syncPolicyService = DefaultSyncPolicyService(),
                    syncStudyDataUseCase = SyncStudyDataUseCase(ForbiddenNetworkSyncGateway()),
                ),
            )

            return CreateTaskClassicFixture(
                database = database,
                topicRepository = topicRepository,
                taskRepository = taskRepository,
                useCase = useCase,
            )
        }
    }
}

private class InMemorySessionLocalDataSource : SessionLocalDataSource {
    private var mode: UserMode = UserMode.LOCAL
    private var lastSyncAt: Long? = null
    private var authorizedUserId: String? = null

    override suspend fun getUserMode(): UserMode = mode
    override suspend fun setUserMode(mode: UserMode) { this.mode = mode }
    override suspend fun getLastSyncAtEpochMs(): Long? = lastSyncAt
    override suspend fun setLastSyncAtEpochMs(value: Long) { lastSyncAt = value }
    override suspend fun clearLastSyncAtEpochMs() { lastSyncAt = null }
    override suspend fun getAuthorizedUserId(): String? = authorizedUserId
    override suspend fun setAuthorizedUserId(value: String?) { authorizedUserId = value }
}

private class RandomIdGenerator : IdGenerator {
    override fun newTopicId(): TopicId = TopicId("topic-${UUID.randomUUID()}")
    override fun newTaskId(): TaskId = TaskId("task-${UUID.randomUUID()}")
    override fun newTaskSolutionId(): TaskSolutionId = TaskSolutionId("solution-${UUID.randomUUID()}")
    override fun newAttachmentId(): AttachmentId = AttachmentId("attachment-${UUID.randomUUID()}")
}

private class SystemClockProvider : ClockProvider {
    @OptIn(ExperimentalTime::class)
    override fun now(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
}

private class ForbiddenNetworkSyncGateway : SyncGateway {
    override suspend fun requestSync(): Nothing = error("Network sync must not be called by classical unit tests")
    override suspend fun requestLoginSync(): Nothing = error("Network sync must not be called by classical unit tests")
    override suspend fun requestLogoutSync(): Nothing = error("Network sync must not be called by classical unit tests")
}
