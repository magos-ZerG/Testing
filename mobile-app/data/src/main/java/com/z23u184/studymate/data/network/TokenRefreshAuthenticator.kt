package com.z23u184.studymate.data.network

import com.z23u184.studymate.data.network.api.StudyMateAuthApi
import com.z23u184.studymate.data.network.dto.RefreshRequestDto
import com.z23u184.studymate.data.session.AuthTokenLocalDataSource
import com.z23u184.studymate.domain.logging.StudyMateLogger
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenRefreshAuthenticator(
    private val authTokenLocalDataSource: AuthTokenLocalDataSource,
    private val authApi: StudyMateAuthApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            StudyMateLogger.w(TAG, "Token refresh stopped: repeated unauthorized response path=${response.request.url.encodedPath}")
            return null
        }

        val requestToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        synchronized(this) {
            val latestAccessToken = runBlocking { authTokenLocalDataSource.getAccessToken() }

            if (!latestAccessToken.isNullOrBlank() && latestAccessToken != requestToken) {
                StudyMateLogger.i(TAG, "Retrying request with already refreshed token path=${response.request.url.encodedPath}")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestAccessToken")
                    .build()
            }

            val refreshToken = runBlocking { authTokenLocalDataSource.getRefreshToken() } ?: run {
                StudyMateLogger.w(TAG, "Token refresh skipped: refresh token is missing")
                return null
            }

            StudyMateLogger.i(TAG, "Refreshing access token")
            val newTokens = runCatching {
                runBlocking {
                    authApi.refresh(RefreshRequestDto(refreshToken))
                }
            }.onFailure { error ->
                StudyMateLogger.e(TAG, "Token refresh failed; clearing local tokens", error)
            }.getOrNull() ?: run {
                runBlocking { authTokenLocalDataSource.clearTokens() }
                return null
            }

            runBlocking {
                authTokenLocalDataSource.setAccessToken(newTokens.accessToken)
                authTokenLocalDataSource.setRefreshToken(newTokens.refreshToken)
            }

            StudyMateLogger.i(TAG, "Access token refreshed successfully")
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var current = response.priorResponse
        while (current != null) {
            result++
            current = current.priorResponse
        }
        return result
    }

    private companion object {
        const val TAG = "TokenRefreshAuthenticator"
    }
}
