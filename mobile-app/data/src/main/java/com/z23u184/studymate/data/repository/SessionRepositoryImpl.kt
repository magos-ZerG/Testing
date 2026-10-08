package com.z23u184.studymate.data.repository

import com.z23u184.studymate.data.session.SessionLocalDataSource
import com.z23u184.studymate.domain.model.UserMode
import com.z23u184.studymate.domain.repository.SessionRepository

class SessionRepositoryImpl(
    private val sessionLocalDataSource: SessionLocalDataSource
) : SessionRepository {
    override suspend fun getUserMode(): UserMode = sessionLocalDataSource.getUserMode()

    override suspend fun getAuthorizedUserId(): String? = sessionLocalDataSource.getAuthorizedUserId()

    override suspend fun getLastSyncAtEpochMs(): Long? = sessionLocalDataSource.getLastSyncAtEpochMs()

    suspend fun setUserMode(mode: UserMode) = sessionLocalDataSource.setUserMode(mode)
    suspend fun setAuthorizedUser(userId: String?) = sessionLocalDataSource.setAuthorizedUserId(userId)
    suspend fun setLastSyncAtEpochMs(value: Long) = sessionLocalDataSource.setLastSyncAtEpochMs(value)
}
