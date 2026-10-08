package com.z23u184.studymate.data.network

import com.z23u184.studymate.data.session.AuthTokenLocalDataSource
import com.z23u184.studymate.domain.logging.StudyMateLogger
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthHeaderInterceptor(
    private val authTokenLocalDataSource: AuthTokenLocalDataSource,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (originalRequest.header("Authorization") != null) {
            StudyMateLogger.d(TAG, "Authorization header already exists for path=${originalRequest.url.encodedPath}")
            return chain.proceed(originalRequest)
        }

        val token = runBlocking { authTokenLocalDataSource.getAccessToken() }
        val request = if (token.isNullOrBlank()) {
            StudyMateLogger.d(TAG, "Access token is missing for path=${originalRequest.url.encodedPath}")
            originalRequest
        } else {
            StudyMateLogger.d(TAG, "Access token attached for path=${originalRequest.url.encodedPath}")
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        return chain.proceed(request)
    }

    private companion object {
        const val TAG = "AuthHeaderInterceptor"
    }
}
