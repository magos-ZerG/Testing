package com.z23u184.studymate.app

import android.net.Uri
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Black-box app E2E: commands are sent to the *installed* isolated debug APK
 * through Android IPC. No app source classes, Koin, Room or internal repositories
 * are accessed from this test. A separate Retrofit client inspects the real API.
 */
@RunWith(AndroidJUnit4::class)
class StudyMateApplicationE2ETest {
    private val target by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
    private val bridgeUri by lazy { Uri.parse("content://${target.packageName}.lab2e2e") }
    private val serverUrl by lazy {
        InstrumentationRegistry.getArguments().getString("studymate.baseUrl")
            ?: "http://127.0.0.1:8000/" // Requires `adb reverse tcp:8000 tcp:<host API port>`.
    }

    private val backend: BackendApi by lazy {
        val json = Json { ignoreUnknownKeys = true }
        Retrofit.Builder()
            .baseUrl(serverUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .build(),
            )
            .build()
            .create(BackendApi::class.java)
    }

    @Before
    fun resetApp() {
        assertTrue("Only the disposable E2E APK may be tested", target.packageName.endsWith(".lab2e2e"))
        backend.health().execute().requireBodylessSuccess("GET /health; check adb reverse and API startup")
        command("reset")
    }

    @Test
    fun externalCommandsSyncAndRestoreThroughRealApp() {
        val nonce = UUID.randomUUID().toString().replace("-", "")
        val email = "blackbox_$nonce@example.com"
        val password = "TestPass123!"
        val topicTitle = "Black-box topic $nonce"
        val taskTitle = "Black-box task $nonce"
        val description = "Created by the installed app $nonce"

        command("registerLogin", "email" to email, "password" to password)
        val topicId = command("createTopic", "title" to topicTitle).requiredId()
        val taskId = command(
            "createTask",
            "topicId" to topicId,
            "title" to taskTitle,
            "description" to description,
        ).requiredId()

        // Independent backend oracle: this Retrofit client has no app session or DB access.
        val session = backend.login(LoginRequest(email, password))
            .execute().requireBody("POST /api/v1/auth/login")
        val snapshot = backend.pull("Bearer ${session.accessToken}")
            .execute().requireBody("GET /api/v1/sync/pull")

        assertEquals(topicTitle, snapshot.topics.single { it.clientId == topicId }.title)
        val remoteTask = snapshot.tasks.single { it.clientId == taskId }
        assertEquals(taskTitle, remoteTask.title)
        assertEquals(description, remoteTask.description)
        assertEquals(topicId, remoteTask.topicClientId)

        command("logout")
        assertFalse(command("getTopic", "id" to topicId).getBoolean("found"))
        assertFalse(command("getTask", "id" to taskId).getBoolean("found"))

        command("login", "email" to email, "password" to password)
        val restoredTopic = command("getTopic", "id" to topicId)
        val restoredTask = command("getTask", "id" to taskId)
        assertTrue("Topic not recovered through the real application", restoredTopic.getBoolean("found"))
        assertTrue("Task not recovered through the real application", restoredTask.getBoolean("found"))
        assertEquals(topicTitle, restoredTopic.getString("title"))
        assertEquals(taskTitle, restoredTask.getString("title"))
        assertEquals(description, restoredTask.getString("description"))
        assertEquals(topicId, restoredTask.getString("topicId"))
        command("logout")
    }

    private fun command(name: String, vararg arguments: Pair<String, String>): Bundle {
        val extras = Bundle().apply { arguments.forEach { (key, value) -> putString(key, value) } }
        val result = requireNotNull(target.contentResolver.call(bridgeUri, name, null, extras)) {
            "No response from the installed application for $name"
        }
        check(result.getBoolean("ok")) {
            "Application command $name failed: ${result.getString("error")}" 
        }
        return result
    }

    private fun Bundle.requiredId(): String =
        requireNotNull(getString("id")) { "Application did not return an ID" }

    private fun <T : Any> Response<T>.requireBody(action: String): T {
        check(isSuccessful) { "$action failed: HTTP ${code()} ${errorBody()?.string()}" }
        return requireNotNull(body()) { "$action returned an empty body" }
    }

    private fun Response<*>.requireBodylessSuccess(action: String) {
        check(isSuccessful) { "$action failed: HTTP ${code()} ${errorBody()?.string()}" }
    }

    private interface BackendApi {
        @GET("health")
        fun health(): Call<Void>

        @POST("api/v1/auth/login")
        fun login(@Body request: LoginRequest): Call<LoginSession>

        @GET("api/v1/sync/pull")
        fun pull(@Header("Authorization") authorization: String): Call<RemoteSnapshot>
    }
}

@Serializable
private data class LoginRequest(val email: String, val password: String)

@Serializable
private data class LoginSession(val accessToken: String)

@Serializable
private data class RemoteSnapshot(
    val topics: List<RemoteTopic>,
    val tasks: List<RemoteTask>,
)

@Serializable
private data class RemoteTopic(val clientId: String, val title: String)

@Serializable
private data class RemoteTask(
    val clientId: String,
    val topicClientId: String,
    val title: String,
    val description: String? = null,
)
