package com.z23u184.studymate.domain.repository

import com.z23u184.studymate.domain.model.AuthUser

interface AuthRepository {
    suspend fun register(email: String, password: String): AuthUser
    suspend fun login(email: String, password: String): AuthUser
    suspend fun logout()
}
