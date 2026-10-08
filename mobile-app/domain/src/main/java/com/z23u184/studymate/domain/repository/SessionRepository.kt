package com.z23u184.studymate.domain.repository

import com.z23u184.studymate.domain.model.UserMode

interface SessionRepository {
    suspend fun getUserMode(): UserMode

    suspend fun getAuthorizedUserId(): String? = null

    suspend fun getLastSyncAtEpochMs(): Long? = null
}
