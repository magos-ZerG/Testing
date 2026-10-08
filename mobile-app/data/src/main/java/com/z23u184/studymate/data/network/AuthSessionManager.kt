package com.z23u184.studymate.data.network

import com.z23u184.studymate.data.network.api.StudyMateAuthApi
import com.z23u184.studymate.data.network.api.StudyMateAuthorizedAuthApi
import com.z23u184.studymate.data.network.dto.LoginRequestDto
import com.z23u184.studymate.data.network.dto.RefreshRequestDto
import com.z23u184.studymate.data.network.dto.RegisterRequestDto
import com.z23u184.studymate.data.network.dto.UserResponseDto
import com.z23u184.studymate.data.session.AuthTokenLocalDataSource
import com.z23u184.studymate.data.session.LocalStudyDataCleaner
import com.z23u184.studymate.data.session.SessionLocalDataSource
import com.z23u184.studymate.data.sync.StudyDataSyncCoordinator
import com.z23u184.studymate.domain.logging.StudyMateLogger
import com.z23u184.studymate.domain.model.UserMode

class AuthSessionManager(
    private val authApi: StudyMateAuthApi,
    private val authorizedAuthApi: StudyMateAuthorizedAuthApi,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val authTokenLocalDataSource: AuthTokenLocalDataSource,
    private val localStudyDataCleaner: LocalStudyDataCleaner,
    private val syncCoordinator: StudyDataSyncCoordinator
) {
    suspend fun register(email: String, password: String): UserResponseDto {
        StudyMateLogger.i(TAG, "Registration started email=$email")
        return runCatching {
            authApi.register(RegisterRequestDto(email = email, password = password))
        }.onSuccess { user ->
            StudyMateLogger.i(TAG, "Registration completed userId=${user.id} email=${user.email}")
        }.onFailure { error ->
            StudyMateLogger.e(TAG, "Registration failed email=$email", error)
        }.getOrThrow()
    }

    suspend fun login(email: String, password: String): UserResponseDto {
        StudyMateLogger.i(TAG, "Login started email=$email")
        val tokens = runCatching {
            authApi.login(LoginRequestDto(email = email, password = password))
        }.onFailure { error ->
            StudyMateLogger.e(TAG, "Login request failed email=$email", error)
        }.getOrThrow()

        authTokenLocalDataSource.setAccessToken(tokens.accessToken)
        authTokenLocalDataSource.setRefreshToken(tokens.refreshToken)
        StudyMateLogger.d(TAG, "Auth tokens saved after login")

        val me = runCatching { authorizedAuthApi.me() }
            .onFailure { error -> StudyMateLogger.e(TAG, "Failed to load authorized user after login", error) }
            .getOrThrow()

        sessionLocalDataSource.clearLastSyncAtEpochMs()
        sessionLocalDataSource.setAuthorizedUserId(me.id)
        sessionLocalDataSource.setUserMode(UserMode.AUTHORIZED)

        StudyMateLogger.i(TAG, "Login completed userId=${me.id} email=${me.email}")
        return me
    }

    suspend fun logout() {
        val userId = sessionLocalDataSource.getAuthorizedUserId()
        StudyMateLogger.i(TAG, "Logout started userId=$userId")

        val syncResult = syncCoordinator.syncNow(com.z23u184.studymate.data.db.entity.SyncTrigger.AFTER_MUTATION)
        if (syncResult.failedCount > 0) {
            StudyMateLogger.w(TAG, "Logout stopped: sync failed userId=$userId failedCount=${syncResult.failedCount}")
            throw IllegalStateException("Не удалось завершить синхронизацию перед выходом")
        }

        val refreshToken = authTokenLocalDataSource.getRefreshToken()
        if (!refreshToken.isNullOrBlank()) {
            runCatching { authApi.logout(RefreshRequestDto(refreshToken)) }
                .onSuccess { StudyMateLogger.i(TAG, "Remote logout completed userId=$userId") }
                .onFailure { error -> StudyMateLogger.w(TAG, "Remote logout failed; local cleanup will continue userId=$userId", error) }
        } else {
            StudyMateLogger.d(TAG, "Remote logout skipped: refresh token is missing userId=$userId")
        }

        localStudyDataCleaner.clearAccountStudyData()
        authTokenLocalDataSource.clearTokens()
        sessionLocalDataSource.clearLastSyncAtEpochMs()
        sessionLocalDataSource.setAuthorizedUserId(null)
        sessionLocalDataSource.setUserMode(UserMode.LOCAL)
        StudyMateLogger.i(TAG, "Logout completed userId=$userId")
    }

    private companion object {
        const val TAG = "AuthSessionManager"
    }
}
