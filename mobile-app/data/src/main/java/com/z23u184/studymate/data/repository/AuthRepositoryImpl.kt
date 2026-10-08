package com.z23u184.studymate.data.repository

import com.z23u184.studymate.data.network.AuthSessionManager
import com.z23u184.studymate.domain.model.AuthUser
import com.z23u184.studymate.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val authSessionManager: AuthSessionManager,
) : AuthRepository {
    override suspend fun register(email: String, password: String): AuthUser =
        authSessionManager.register(email = email, password = password).toDomain()

    override suspend fun login(email: String, password: String): AuthUser =
        authSessionManager.login(email = email, password = password).toDomain()

    override suspend fun logout() {
        authSessionManager.logout()
    }
}

private fun com.z23u184.studymate.data.network.dto.UserResponseDto.toDomain(): AuthUser =
    AuthUser(
        id = id,
        email = email,
        createdAt = createdAt,
    )
