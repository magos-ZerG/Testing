package com.z23u184.studymate.app

import android.net.Uri
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Black-box E2E. This test does not import application classes or use Koin,
 * Room, use cases, or view models. All application interactions go through
 * its debug-only Android IPC contract. Retrofit independently verifies the
 * observable server state. No Activity is launched and no UI is tested.
 */
@RunWith(AndroidJUnit4::class)
class StudyMateApplicationE2ETest {
    private val target by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
    private val bridgeUri by lazy { Uri.parse("content://${target.packageName}.lab2e2e") }
    private val serverUrl by lazy {
        InstrumentationRegistry.getArguments().getString("studymate.baseUrl")
            ?: "http://127.0.0.1:8000/"
    }

    private val backend: BackendApi by lazy {
        Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .build()
            )
            .build()
            .create(BackendApi::class.java)
    }

    @Before
    fun resetApp() {
        assertTrue("Only the separate E2E APK may be tested", target.packageName.endsWith(".lab2e2e"))
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

        val loginBody = JSONObject().put("email", email).put("password", password)
            .toString().toRequestBody("application/json".toMediaType())
        val loginResponse = backend.login(loginBody).execute()
        assertTrue("Backend login failed: HTTP ${loginResponse.code()}", loginResponse.isSuccessful)
        val accessToken = JSONObject(loginResponse.body()!!.string()).getString("accessToken")

        val pullResponse = backend.pull("Bearer $accessToken").execute()
        assertTrue("Backend pull failed: HTTP ${pullResponse.code()}", pullResponse.isSuccessful)
        val remote = JSONObject(pullResponse.body()!!.string())
        assertEquals(topicTitle, remote.getJSONArray("topics").findByClientId(topicId).getString("title"))
        val remoteTask = remote.getJSONArray("tasks").findByClientId(taskId)
        assertEquals(taskTitle, remoteTask.getString("title"))
        assertEquals(description, remoteTask.getString("description"))
        
        command("logout")
        assertFalse(command("getTopic", "id" to topicId).getBoolean("found"))
        assertFalse(command("getTask", "id" to taskId).getBoolean("found"))

        command("login", "email" to email, "password" to password)
        val restoredTopic = command("getTopic", "id" to topicId)
        val restoredTask = command("getTask", "id" to taskId)
        assertTrue("Topic not recovered from PostgreSQL", restoredTopic.getBoolean("found"))
        assertTrue("Task not recovered from PostgreSQL", restoredTask.getBoolean("found"))
        assertEquals(topicTitle, restoredTopic.getString("title"))
        assertEquals(taskTitle, restoredTask.getString("title"))
        assertEquals(description, restoredTask.getString("description"))
        assertEquals(topicId, restoredTask.getString("topicId"))
        command("logout")
    }

    private fun command(name: String, vararg arguments: Pair<String, String>): Bundle {
        val extras = Bundle().apply { arguments.forEach { (key, value) -> putString(key, value) } }
        val result = target.contentResolver.call(bridgeUri, name, null, extras)
        assertNotNull("No response from the installed application for $name", result)
        check(result!!.getBoolean("ok")) { "Application command $name failed: ${result.getString("error")}" }
        return result
    }

    private fun Bundle.requiredId(): String = requireNotNull(getString("id")) { "Application did not return an ID" }

    private fun JSONArray.findByClientId(id: String): JSONObject {
        for (index in 0 until length()) {
            val entity = getJSONObject(index)
            if (entity.optString("clientId") == id) return entity
        }
        error("Server does not contain the application-created record: $id")
    }

    /** Server's public HTTP contract; intentionally independent of the :data Retrofit services. */
    private interface BackendApi {
        @POST("api/v1/auth/login")
        fun login(@Body request: RequestBody): Call<ResponseBody>

        @GET("api/v1/sync/pull")
        fun pull(@Header("Authorization") authorization: String): Call<ResponseBody>
    }
}
