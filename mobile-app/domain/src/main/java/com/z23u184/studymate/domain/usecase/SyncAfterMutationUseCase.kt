package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.repository.SessionRepository
import com.z23u184.studymate.domain.service.SyncPolicyService

class SyncAfterMutationUseCase(
    private val sessionRepository: SessionRepository,
    private val syncPolicyService: SyncPolicyService,
    private val syncStudyDataUseCase: SyncStudyDataUseCase
) {
    suspend operator fun invoke(): DomainResult<Unit> {
        val userMode = sessionRepository.getUserMode()
        if (!syncPolicyService.shouldSyncAfterMutation(userMode)) {
            return DomainResult.Success(Unit)
        }
        return syncStudyDataUseCase()
    }
}
