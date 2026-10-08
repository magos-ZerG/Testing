package com.z23u184.studymate.data.datasource.local

import com.z23u184.studymate.data.db.dao.SyncSessionDao
import com.z23u184.studymate.data.db.entity.SyncSessionEntity
import com.z23u184.studymate.data.db.entity.SyncSessionStatus

class SyncSessionLocalDataSource(private val dao: SyncSessionDao) {
    suspend fun insert(session: SyncSessionEntity): Long = dao.insert(session)
    suspend fun finishSession(id: Long, finishedAtEpochMs: Long, status: SyncSessionStatus, pushedCount: Int, pulledCount: Int, failedCount: Int, errorMessage: String?) =
        dao.finishSession(id, finishedAtEpochMs, status, pushedCount, pulledCount, failedCount, errorMessage)
    suspend fun getLatestSuccessfulSession(): SyncSessionEntity? = dao.getLatestSuccessfulSession()
}
