package com.z23u184.studymate.data.sync

data class SyncResult(
    val pushedCount: Int,
    val pulledCount: Int,
    val failedCount: Int,
    val hadChanges: Boolean
) {
    val isSuccess: Boolean get() = failedCount == 0
    val shouldRetry: Boolean get() = failedCount > 0
}
