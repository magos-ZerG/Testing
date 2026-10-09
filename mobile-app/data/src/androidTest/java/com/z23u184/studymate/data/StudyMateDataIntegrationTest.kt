package com.z23u184.studymate.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.z23u184.studymate.data.datasource.local.AttachmentLocalDataSource
import com.z23u184.studymate.data.datasource.local.FlashcardBestResultLocalDataSource
import com.z23u184.studymate.data.datasource.local.SyncQueueLocalDataSource
import com.z23u184.studymate.data.datasource.local.SyncSessionLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskSolutionLocalDataSource
import com.z23u184.studymate.data.datasource.local.TopicLocalDataSource
import com.z23u184.studymate.data.db.StudyMateDatabase
import com.z23u184.studymate.data.db.entity.SyncTrigger
import com.z23u184.studymate.data.file.AttachmentFileManager
import com.z23u184.studymate.data.network.AuthHeaderInterceptor
import com.z23u184.studymate.data.network.AuthSessionManager
import com.z23u184.studymate.data.network.api.StudyMateAuthApi
import com.z23u184.studymate.data.network.api.StudyMateAuthorizedAuthApi
import com.z23u184.studymate.data.network.api.StudyMateSyncApi
import com.z23u184.studymate.data.network.dto.PushChangesRequestDto
import com.z23u184.studymate.data.network.dto.RemoteTopicPushDto
import com.z23u184.studymate.data.repository.SessionRepositoryImpl
import com.z23u184.studymate.data.repository.TaskRepositoryImpl
import com.z23u184.studymate.data.repository.TopicRepositoryImpl
import com.z23u184.studymate.data.session.LocalStudyDataCleaner
import com.z23u184.studymate.data.session.SessionPreferencesDataSource
import com.z23u184.studymate.data.sync.StudyDataSyncCoordinator
import com.z23u184.studymate.data.sync.gateway.SyncGatewayImpl
import com.z23u184.studymate.data.sync.merge.SyncConflictResolver
import com.z23u184.studymate.data.sync.queue.QueueMutationMerger
import com.z23u184.studymate.data.sync.queue.SyncQueuePlanner
import com.z23u184.studymate.data.sync.remote.RetrofitSyncRemoteDataSource
import com.z23u184.studymate.data.sync.worker.ImmediateSyncWorkScheduler
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.AttachmentId
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TaskSolutionId
import com.z23u184.studymate.domain.model.TaskStatus
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.model.UserMode
import com.z23u184.studymate.domain.service.ClockProvider
import com.z23u184.studymate.domain.service.DefaultSyncPolicyService
import com.z23u184.studymate.domain.service.DefaultTaskStatusTransitionPolicy
import com.z23u184.studymate.domain.service.IdGenerator
import com.z23u184.studymate.domain.usecase.CreateTaskUseCase
import com.z23u184.studymate.domain.usecase.CreateTopicUseCase
import com.z23u184.studymate.domain.usecase.DeleteTopicUseCase
import com.z23u184.studymate.domain.usecase.GetTaskByIdUseCase
import com.z23u184.studymate.domain.usecase.GetTopicByIdUseCase
import com.z23u184.studymate.domain.usecase.SyncAfterMutationUseCase
import com.z23u184.studymate.domain.usecase.SyncStudyDataUseCase
import com.z23u184.studymate.domain.usecase.UpdateTaskUseCase
import com.z23u184.studymate.domain.validation.DefaultTaskValidator
import com.z23u184.studymate.domain.validation.DefaultTopicValidator
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class RealServerDataIntegrationTest {

    private lateinit var context: Context
    private lateinit var runtime: TestRuntime

    private lateinit var sessionPreferencesDataSource: SessionPreferencesDataSource

    @Before
    fun setUp() {
        context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .applicationContext

        if (!::sessionPreferencesDataSource.isInitialized) {
            sessionPreferencesDataSource = SessionPreferencesDataSource(context)
        }

        runtime = TestRuntime.create(
            context = context,
            baseUrl = resolveBaseUrl(),
            sessionPreferences = sessionPreferencesDataSource,
        )

        runBlocking { runtime.resetLocalSession() }
    }

    @After
    fun tearDown() {
        runBlocking {
            runCatching { runtime.authSessionManager.logout() }
            runtime.resetLocalSession()
        }
        runtime.close()
    }

    @Test
    fun login_initialSync_pullsRemoteTopic_and_logoutClearsSession() = runTest {
        val credentials = TestCredentials.unique()

        runtime.authSessionManager.register(credentials.email, credentials.password)
        runtime.authSessionManager.login(credentials.email, credentials.password)

        val remoteSeedClientId = "seed-topic-${UUID.randomUUID()}"
        val remoteSeedTitle = "Remote seed ${UUID.randomUUID()}"
        runtime.syncApi.pushChanges(
            PushChangesRequestDto(
                topics = listOf(
                    RemoteTopicPushDto(
                        clientId = remoteSeedClientId,
                        title = remoteSeedTitle,
                        updatedAt = java.time.Instant.now().toString(),
                        operation = "CREATE",
                    )
                )
            )
        )
        runtime.authSessionManager.logout()
        runtime.resetLocalSession()
        runtime.clearLocalDatabase()

        val me = runtime.authSessionManager.login(credentials.email, credentials.password)
        val syncResult = runtime.syncCoordinator.syncNow(SyncTrigger.LOGIN)

        assertEquals(0, syncResult.failedCount)
        assertTrue(syncResult.pulledCount >= 1)
        assertEquals(UserMode.AUTHORIZED, runtime.sessionRepository.getUserMode())
        assertEquals(me.id, runtime.sessionRepository.getAuthorizedUserId())

        val pulledTopic = runtime.topicRepository.getById(TopicId(remoteSeedClientId))
        assertNotNull(pulledTopic)
        assertEquals(remoteSeedTitle, pulledTopic?.title)

        runtime.authSessionManager.logout()

        assertEquals(UserMode.LOCAL, runtime.sessionRepository.getUserMode())
        assertNull(runtime.sessionRepository.getAuthorizedUserId())
    }

    @Test
    fun createTopic_useCase_savesLocally_andPushesToRealServer() = runTest {
        val credentials = TestCredentials.unique()
        runtime.authSessionManager.register(credentials.email, credentials.password)
        runtime.authSessionManager.login(credentials.email, credentials.password)
        runtime.syncCoordinator.syncNow(SyncTrigger.LOGIN)

        val createTopicUseCase = runtime.createTopicUseCase()
        val title = "Topic ${UUID.randomUUID()}"

        val created = createTopicUseCase(title).requireSuccess()
        val localTopic = runtime.topicRepository.getById(created.id)
        val pulled = runtime.syncApi.pullChanges(null)
        val remoteTopic = pulled.topics.firstOrNull { it.clientId == created.id.value && !it.isDeleted }
        val queueItems = runtime.database.syncQueueDao().getProcessable(System.currentTimeMillis())

        assertNotNull(localTopic)
        assertEquals(title, localTopic?.title)
        assertNotNull(runtime.database.topicDao().getById(created.id.value)?.remoteId)
        assertNotNull(remoteTopic)
        assertEquals(title, remoteTopic?.title)
        assertTrue(queueItems.isEmpty())
    }

    @Test
    fun createUpdateDeleteTask_flow_syncsLocalAndRemoteState() = runTest {
        val credentials = TestCredentials.unique()
        runtime.authSessionManager.register(credentials.email, credentials.password)
        runtime.authSessionManager.login(credentials.email, credentials.password)
        runtime.syncCoordinator.syncNow(SyncTrigger.LOGIN)

        val createTopicUseCase = runtime.createTopicUseCase()
        val createTaskUseCase = runtime.createTaskUseCase()
        val updateTaskUseCase = runtime.updateTaskUseCase()
        val getTaskByIdUseCase = GetTaskByIdUseCase(runtime.taskRepository)
        val deleteTopicUseCase = runtime.deleteTopicUseCase()

        val topic = createTopicUseCase("Topic ${UUID.randomUUID()}").requireSuccess()
        val task = createTaskUseCase(
            topic.id,
            "Task ${UUID.randomUUID()}",
            "Description ${UUID.randomUUID()}"
        ).requireSuccess()

        val updatedTitle = "Updated task ${UUID.randomUUID()}"
        val updatedDescription = "Updated description ${UUID.randomUUID()}"
        val updatedTask = updateTaskUseCase(
            taskId = task.id,
            title = updatedTitle,
            description = updatedDescription,
            status = TaskStatus.PLANNED,
            deadlineAt = task.deadlineAt
        ).requireSuccess()

        assertEquals(updatedTitle, getTaskByIdUseCase(task.id)?.title)
        assertEquals(updatedDescription, getTaskByIdUseCase(task.id)?.description)

        deleteTopicUseCase(topic.id).requireSuccess()

        assertNull(getTaskByIdUseCase(task.id))
        assertNull(GetTopicByIdUseCase(runtime.topicRepository)(topic.id))

        val pulled = runtime.syncApi.pullChanges(null)
        val remoteTask = pulled.tasks.firstOrNull { it.clientId == updatedTask.id.value }
        val remoteTopic = pulled.topics.firstOrNull { it.clientId == topic.id.value }

        assertNotNull(remoteTask)
        assertTrue(remoteTask!!.isDeleted)
        assertEquals(updatedTitle, remoteTask.title)
        assertNotNull(remoteTopic)
        assertTrue(remoteTopic!!.isDeleted)
        assertTrue(runtime.database.syncQueueDao().getProcessable(System.currentTimeMillis()).isEmpty())
    }

    private fun resolveBaseUrl(): String {
        val args = InstrumentationRegistry.getArguments()
        val host = args.getString("studymate.baseUrl") ?: "http://10.0.2.2:8000/"
        return if (host.endsWith('/')) host else "$host/"
    }

    private data class TestCredentials(
        val email: String,
        val password: String,
    ) {
        companion object {
            fun unique(): TestCredentials {
                val id = UUID.randomUUID().toString().replace("-", "")
                return TestCredentials(
                    email = "studymate_it_${id}@example.com",
                    password = "TestPass123!",
                )
            }
        }
    }

    private class TestRuntime private constructor(
        val database: StudyMateDatabase,
        val sessionPreferencesDataSource: SessionPreferencesDataSource,
        val authSessionManager: AuthSessionManager,
        val syncApi: StudyMateSyncApi,
        val topicRepository: TopicRepositoryImpl,
        val taskRepository: TaskRepositoryImpl,
        val sessionRepository: SessionRepositoryImpl,
        val syncCoordinator: StudyDataSyncCoordinator,
    ) {
        suspend fun resetLocalSession() {
            sessionPreferencesDataSource.clearTokens()
            sessionPreferencesDataSource.setAuthorizedUserId(null)
            sessionPreferencesDataSource.setUserMode(UserMode.LOCAL)
            sessionPreferencesDataSource.clearLastSyncAtEpochMs()
            sessionPreferencesDataSource.setLastSuccessfulSyncAtEpochMs(null)
        }

        fun clearLocalDatabase() {
            database.clearAllTables()
        }

        fun createTopicUseCase(): CreateTopicUseCase = CreateTopicUseCase(
            topicRepository = topicRepository,
            topicValidator = DefaultTopicValidator(),
            idGenerator = TestIdGenerator(),
            clockProvider = SystemClockProvider(),
            syncAfterMutationUseCase = syncAfterMutationUseCase(),
        )

        fun createTaskUseCase(): CreateTaskUseCase = CreateTaskUseCase(
            topicRepository = topicRepository,
            taskRepository = taskRepository,
            taskValidator = DefaultTaskValidator(),
            idGenerator = TestIdGenerator(),
            clockProvider = SystemClockProvider(),
            syncAfterMutationUseCase = syncAfterMutationUseCase(),
        )

        fun updateTaskUseCase(): UpdateTaskUseCase = UpdateTaskUseCase(
            taskRepository = taskRepository,
            taskValidator = DefaultTaskValidator(),
            clockProvider = SystemClockProvider(),
            syncAfterMutationUseCase = syncAfterMutationUseCase(),
            taskStatusTransitionPolicy = DefaultTaskStatusTransitionPolicy(),
        )

        fun deleteTopicUseCase(): DeleteTopicUseCase = DeleteTopicUseCase(
            topicRepository = topicRepository,
            syncAfterMutationUseCase = syncAfterMutationUseCase(),
        )

        private fun syncAfterMutationUseCase(): SyncAfterMutationUseCase = SyncAfterMutationUseCase(
            sessionRepository = sessionRepository,
            syncPolicyService = DefaultSyncPolicyService(),
            syncStudyDataUseCase = SyncStudyDataUseCase(
                SyncGatewayImpl(
                    syncWorkScheduler = ImmediateSyncWorkScheduler { trigger ->
                        syncCoordinator.syncNow(trigger)
                    },
                    syncCoordinator = syncCoordinator,
                ),
            ),
        )

        fun close() {
            database.close()
        }

        companion object {
            fun create(
                context: Context,
                baseUrl: String,
                sessionPreferences: SessionPreferencesDataSource,
            ): TestRuntime {
                val json = Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                }
                val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(logging)
                    .addInterceptor(AuthHeaderInterceptor(sessionPreferences))
                    .build()
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                    .build()

                val database = Room.inMemoryDatabaseBuilder(context, StudyMateDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()

                val topicLocal = TopicLocalDataSource(database.topicDao())
                val taskLocal = TaskLocalDataSource(database.taskDao())
                val solutionLocal = TaskSolutionLocalDataSource(database.taskSolutionDao())
                val attachmentLocal = AttachmentLocalDataSource(database.attachmentDao())
                val flashcardBestResultLocal = FlashcardBestResultLocalDataSource(database.flashcardBestResultDao())
                val syncQueueLocal = SyncQueueLocalDataSource(database.syncQueueDao())
                val syncSessionLocal = SyncSessionLocalDataSource(database.syncSessionDao())
                val queuePlanner = SyncQueuePlanner(QueueMutationMerger())
                val syncCoordinator = StudyDataSyncCoordinator(
                    database = database,
                    topicLocalDataSource = topicLocal,
                    taskLocalDataSource = taskLocal,
                    taskSolutionLocalDataSource = solutionLocal,
                    attachmentLocalDataSource = attachmentLocal,
                    flashcardBestResultLocalDataSource = flashcardBestResultLocal,
                    syncQueueLocalDataSource = syncQueueLocal,
                    syncSessionLocalDataSource = syncSessionLocal,
                    sessionLocalDataSource = sessionPreferences,
                    syncRemoteDataSource = RetrofitSyncRemoteDataSource(retrofit.create(StudyMateSyncApi::class.java)),
                    syncConflictResolver = SyncConflictResolver(),
                )

                return TestRuntime(
                    database = database,
                    sessionPreferencesDataSource = sessionPreferences,
                    authSessionManager = AuthSessionManager(
                        authApi = retrofit.create(StudyMateAuthApi::class.java),
                        authorizedAuthApi = retrofit.create(StudyMateAuthorizedAuthApi::class.java),
                        sessionLocalDataSource = sessionPreferences,
                        authTokenLocalDataSource = sessionPreferences,
                        localStudyDataCleaner = LocalStudyDataCleaner(
                            database = database,
                            attachmentFileManager = AttachmentFileManager(context),
                        ),
                        syncCoordinator = syncCoordinator,
                    ),
                    syncApi = retrofit.create(StudyMateSyncApi::class.java),
                    topicRepository = TopicRepositoryImpl(
                        database = database,
                        topicLocalDataSource = topicLocal,
                        taskLocalDataSource = taskLocal,
                        taskSolutionLocalDataSource = solutionLocal,
                        attachmentLocalDataSource = attachmentLocal,
                        flashcardBestResultLocalDataSource = flashcardBestResultLocal,
                        syncQueueLocalDataSource = syncQueueLocal,
                        syncQueuePlanner = queuePlanner,
                    ),
                    taskRepository = TaskRepositoryImpl(
                        database = database,
                        taskLocalDataSource = taskLocal,
                        taskSolutionLocalDataSource = solutionLocal,
                        attachmentLocalDataSource = attachmentLocal,
                        syncQueueLocalDataSource = syncQueueLocal,
                        syncQueuePlanner = queuePlanner,
                    ),
                    sessionRepository = SessionRepositoryImpl(sessionPreferences),
                    syncCoordinator = syncCoordinator,
                )
            }
        }
    }
}

private fun <T> DomainResult<T>.requireSuccess(): T = when (this) {
    is DomainResult.Success -> value
    is DomainResult.Failure -> error("Expected success, got $error")
}

private class TestIdGenerator : IdGenerator {
    override fun newTopicId(): TopicId = TopicId("topic-${UUID.randomUUID()}")
    override fun newTaskId(): TaskId = TaskId("task-${UUID.randomUUID()}")
    override fun newTaskSolutionId(): TaskSolutionId = TaskSolutionId("solution-${UUID.randomUUID()}")
    override fun newAttachmentId(): AttachmentId = AttachmentId("attachment-${UUID.randomUUID()}")
}

@OptIn(ExperimentalTime::class)
private class SystemClockProvider : ClockProvider {
    override fun now(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
}
