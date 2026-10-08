package com.z23u184.studymate.domain.usecase

import com.z23u184.studymate.domain.DomainResult
import com.z23u184.studymate.domain.service.SyncGateway

class SyncStudyDataUseCase(
    private val syncGateway: SyncGateway
) {
    suspend operator fun invoke(): DomainResult<Unit> {
        syncGateway.requestSync()
        return DomainResult.Success(Unit)
    }
}
