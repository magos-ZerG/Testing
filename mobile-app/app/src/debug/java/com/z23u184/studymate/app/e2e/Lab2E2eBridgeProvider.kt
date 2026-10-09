package com.z23u184.studymate.app.e2e

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.z23u184.studymate.data.db.entity.SyncTrigger
import com.z23u184.studymate.data.network.AuthSessionManager
import com.z23u184.studymate.data.session.LocalStudyDataCleaner
import com.z23u184.studymate.data.session.SessionPreferencesDataSource
import com.z23u184.studymate.data.sync.StudyDataSyncCoordinator
import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.model.TopicId
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.UserMode
import com.z23u184.studymate.domain.usecase.CreateTaskUseCase
import com.z23u184.studymate.domain.usecase.CreateTopicUseCase
import com.z23u184.studymate.domain.usecase.GetTaskByIdUseCase
import com.z23u184.studymate.domain.usecase.GetTopicByIdUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

/**
 * Debug-only black-box command endpoint. It is DISABLED in normal debug APKs,
 * absent from release APKs, and enabled only with -Plab2E2e=true.
 *
 * E2E clients know only operation names and primitive Bundle values: no
 * dependency on the application's Koin graph, Room, or domain types.
 */
class Lab2E2eBridgeProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle = try {
        runBlocking(Dispatchers.IO) {
            val koin = GlobalContext.get()
            val reply = when (method) {
                "reset" -> {
                    val preferences = koin.get<SessionPreferencesDataSource>()
                    preferences.clearTokens()
                    preferences.setAuthorizedUserId(null)
                    preferences.setUserMode(UserMode.LOCAL)
                    preferences.clearLastSyncAtEpochMs()
                    preferences.setLastSuccessfulSyncAtEpochMs(null)
                    koin.get<LocalStudyDataCleaner>().clearAccountStudyData()
                    Bundle()
                }

                "registerLogin" -> {
                    val auth = koin.get<AuthSessionManager>()
                    auth.register(extras.required("email"), extras.required("password"))
                    auth.login(extras.required("email"), extras.required("password"))
                    ensureSynced(koin.get(), SyncTrigger.LOGIN)
                    Bundle()
                }

                "createTopic" -> {
                    val topic = koin.get<CreateTopicUseCase>()(extras.required("title")).valueOrThrow()
                    ensureSynced(koin.get(), SyncTrigger.AFTER_MUTATION)
                    Bundle().apply { putString("id", topic.id.value) }
                }

                "createTask" -> {
                    val task = koin.get<CreateTaskUseCase>()(
                        topicId = TopicId(extras.required("topicId")),
                        title = extras.required("title"),
                        description = extras.required("description"),
                    ).valueOrThrow()
                    ensureSynced(koin.get(), SyncTrigger.AFTER_MUTATION)
                    Bundle().apply { putString("id", task.id.value) }
                }

                "logout" -> {
                    koin.get<AuthSessionManager>().logout()
                    Bundle()
                }

                "login" -> {
                    koin.get<AuthSessionManager>().login(
                        extras.required("email"),
                        extras.required("password"),
                    )
                    ensureSynced(koin.get(), SyncTrigger.LOGIN)
                    Bundle()
                }

                "getTopic" -> {
                    val topic = koin.get<GetTopicByIdUseCase>()(TopicId(extras.required("id")))
                    Bundle().apply {
                        putBoolean("found", topic != null)
                        if (topic != null) putString("title", topic.title)
                    }
                }

                "getTask" -> {
                    val task = koin.get<GetTaskByIdUseCase>()(TaskId(extras.required("id")))
                    Bundle().apply {
                        putBoolean("found", task != null)
                        if (task != null) {
                            putString("title", task.title)
                            putString("description", task.description)
                            putString("topicId", task.topicId.value)
                        }
                    }
                }

                else -> error("Unknown E2E command: $method")
            }
            reply.apply { putBoolean("ok", true) }
        }
    } catch (failure: Exception) {
        Bundle().apply {
            putBoolean("ok", false)
            putString("error", "${failure.javaClass.simpleName}: ${failure.message}")
        }
    }

    private fun Bundle?.required(name: String): String =
        requireNotNull(this?.getString(name)) { "Missing E2E argument: $name" }

    private suspend fun ensureSynced(sync: StudyDataSyncCoordinator, trigger: SyncTrigger) {
        val result = sync.syncNow(trigger)
        check(result.failedCount == 0) { "Synchronization failed: ${result.failedCount} records" }
    }

    private fun <T> DomainResult<T>.valueOrThrow(): T = when (this) {
        is DomainResult.Success -> value
        is DomainResult.Failure -> error("Application operation failed: $error")
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = throw UnsupportedOperationException("Use call()")

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException("Use call()")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Use call()")
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Use call()")
}
