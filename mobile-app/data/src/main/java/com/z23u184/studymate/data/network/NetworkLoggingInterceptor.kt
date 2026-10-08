package com.z23u184.studymate.data.network

import com.z23u184.studymate.domain.logging.StudyMateLogger
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.system.measureTimeMillis

class NetworkLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val method = request.method
        val path = request.url.encodedPath

        StudyMateLogger.i(TAG, "HTTP request started: method=$method path=$path")

        var response: Response? = null
        val elapsedMs = try {
            measureTimeMillis {
                response = chain.proceed(request)
            }
        } catch (error: IOException) {
            StudyMateLogger.e(TAG, "HTTP request failed: method=$method path=$path", error)
            throw error
        }

        val completedResponse = response ?: error("Response is missing")
        val status = completedResponse.code
        val message = "HTTP response received: method=$method path=$path status=$status durationMs=$elapsedMs"
        if (status >= 500) {
            StudyMateLogger.e(TAG, message)
        } else if (status >= 400) {
            StudyMateLogger.w(TAG, message)
        } else {
            StudyMateLogger.i(TAG, message)
        }

        return completedResponse
    }

    private companion object {
        const val TAG = "Network"
    }
}
