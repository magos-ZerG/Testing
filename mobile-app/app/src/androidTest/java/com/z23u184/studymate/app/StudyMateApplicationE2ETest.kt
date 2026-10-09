package com.z23u184.studymate.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.z23u184.studymate.data.db.StudyMateDatabase
import com.z23u184.studymate.data.db.entity.SyncTrigger
import com.z23u184.studymate.data.network.AuthSessionManager
import com.z23u184.studymate.data.network.api.StudyMateSyncApi
import com.z23u184.studymate.data.session.SessionPreferencesDataSource
import com.z23u184.studymate.data.sync.StudyDataSyncCoordinator
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.UserMode
import com.z23u184.studymate.domain.usecase.CreateTaskUseCase
import com.z23u184.studymate.domain.usecase.CreateTopicUseCase
import com.z23u184.studymate.domain.usecase.GetTaskByIdUseCase
import com.z23u184.studymate.domain.usecase.GetTopicByIdUseCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StudyMateApplicationE2ETest {
    private val koin get() = GlobalContext.get()

    @Before
    fun setUp() = runBlocking {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue("Must run against the REAL StudyMate application", target.applicationContext is StudyMateApplication)
        assertTrue("E2E must not overwrite the normal user's APK", target.packageName.endsWith(".lab2e2e"))
        assertEquals("http://127.0.0.1:8000/", BuildConfig.SERVER_BASE_URL)

        val preferences = koin.get<SessionPreferencesDataSource>()
        preferences.clearTokens()
        preferences.setAuthorizedUserId(null)
        preferences.setUserMode(UserMode.LOCAL)
        preferences.clearLastSyncAtEpochMs()
        preferences.setLastSuccessfulSyncAtEpochMs(null)
        koin.get<StudyMateDatabase>().clearAllTables()
    }

    @After
    fun tearDown() = runBlocking {
        if (GlobalContext.getOrNull() != null) {
            runCatching { koin.get<AuthSessionManager>().logout() }
            val preferences = koin.get<SessionPreferencesDataSource>()
            preferences.clearTokens()
            preferences.setAuthorizedUserId(null)
            preferences.setUserMode(UserMode.LOCAL)
            preferences.clearLastSyncAtEpochMs()
            preferences.setLastSuccessfulSyncAtEpochMs(null)
            koin.get<StudyMateDatabase>().clearAllTables()
        }
    }

    @Test
    fun createOnInstalledApp_thenRestoreFromRealServerWithoutUi() = runBlocking {
        val nonce = UUID.randomUUID().toString().replace("-", "")
        val email = "android_e2e_${nonce}@example.com"
        val password = "TestPass123!"
        val topicTitle = "Android topic $nonce"
        val taskTitle = "Android task $nonce"
        val description = "Created in StudyMate APK $nonce"

        val auth = koin.get<AuthSessionManager>()
        val sync = koin.get<StudyDataSyncCoordinator>()
        val server = koin.get<StudyMateSyncApi>()
        val topics = koin.get<GetTopicByIdUseCase>()
        val tasks = koin.get<GetTaskByIdUseCase>()

        auth.register(email, password)
        auth.login(email, password)
        assertEquals(0, sync.syncNow(SyncTrigger.LOGIN).failedCount)

        val topic = koin.get<CreateTopicUseCase>()(topicTitle).valueOrFail()
        val task = koin.get<CreateTaskUseCase>()(topic.id, taskTitle, description).valueOrFail()
        assertEquals(taskTitle, tasks(task.id)?.title)

        assertEquals(0, sync.syncNow(SyncTrigger.AFTER_MUTATION).failedCount)
        val remote = server.pullChanges(null)
        assertEquals(topicTitle, remote.topics.single { it.clientId == topic.id.value }.title)
        assertEquals(taskTitle, remote.tasks.single { it.clientId == task.id.value }.title)

        auth.logout()
        assertNull(topics(topic.id))
        assertNull(tasks(task.id))

        auth.login(email, password)
        val loaded = sync.syncNow(SyncTrigger.LOGIN)
        assertEquals("Server -> Android sync failed", 0, loaded.failedCount)
        assertTrue("Nothing was restored from PostgreSQL", loaded.pulledCount >= 2)
        assertNotNull("Topic missing after fresh login", topics(topic.id))
        assertNotNull("Task missing after fresh login", tasks(task.id))
        assertEquals(topicTitle, topics(topic.id)?.title)
        assertEquals(taskTitle, tasks(task.id)?.title)
        assertEquals(description, tasks(task.id)?.description)
        assertEquals(topic.id, tasks(task.id)?.topicId)
    }
}

private fun <T> DomainResult<T>.valueOrFail(): T = when (this) {
    is DomainResult.Success -> value
    is DomainResult.Failure -> error("Expected successful Android operation, got $error")
}
