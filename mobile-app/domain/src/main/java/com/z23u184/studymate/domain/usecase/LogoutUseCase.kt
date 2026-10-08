package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.repository.AuthRepository

class LogoutUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() = authRepository.logout()
}
