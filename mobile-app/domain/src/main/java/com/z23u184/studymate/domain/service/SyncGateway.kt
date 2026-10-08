package com.z23u184.studymate.domain.service

interface SyncGateway {
    suspend fun requestSync()

    suspend fun requestLoginSync() {
        requestSync()
    }

    suspend fun requestLogoutSync() {
        requestSync()
    }
}
