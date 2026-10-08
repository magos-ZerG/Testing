package com.z23u184.studymate.domain.service

import com.z23u184.studymate.domain.model.UserMode

interface SyncPolicyService {
    fun shouldSyncAfterMutation(userMode: UserMode): Boolean
    fun shouldSyncOnLogin(userMode: UserMode): Boolean
}
