package com.z23u184.studymate.data.session

import com.z23u184.studymate.domain.model.UserMode

interface SessionLocalDataSource {
    suspend fun getUserMode(): UserMode
    suspend fun setUserMode(mode: UserMode)
    suspend fun getLastSyncAtEpochMs(): Long?
    suspend fun setLastSyncAtEpochMs(value: Long)
    suspend fun clearLastSyncAtEpochMs()
    suspend fun getAuthorizedUserId(): String?
    suspend fun setAuthorizedUserId(value: String?)
}
