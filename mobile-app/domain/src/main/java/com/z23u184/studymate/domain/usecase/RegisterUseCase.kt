package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.model.AuthUser
import com.z23u184.studymate.domain.repository.AuthRepository

class RegisterUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): AuthUser =
        authRepository.register(email = email, password = password)
}
