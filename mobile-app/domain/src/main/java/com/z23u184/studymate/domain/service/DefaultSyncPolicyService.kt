package com.z23u184.studymate.domain.service

import com.z23u184.studymate.domain.model.UserMode

class DefaultSyncPolicyService : SyncPolicyService {
    override fun shouldSyncAfterMutation(userMode: UserMode): Boolean {
        return userMode == UserMode.AUTHORIZED
    }

    override fun shouldSyncOnLogin(userMode: UserMode): Boolean {
        return userMode == UserMode.AUTHORIZED
    }
}
