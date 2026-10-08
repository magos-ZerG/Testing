package com.z23u184.studymate.data.session

interface AuthTokenLocalDataSource {
    suspend fun getAccessToken(): String?
    suspend fun setAccessToken(value: String?)
    suspend fun getRefreshToken(): String?
    suspend fun setRefreshToken(value: String?)
    suspend fun clearTokens()
}
