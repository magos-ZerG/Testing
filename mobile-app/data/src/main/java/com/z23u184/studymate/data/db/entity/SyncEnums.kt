package com.z23u184.studymate.data.db.entity

enum class SyncEntityType {
    TOPIC,
    TASK,
    TASK_SOLUTION,
    ATTACHMENT,
    FLASHCARD_BEST_RESULT
}

enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}

enum class SyncQueueStatus {
    PENDING,
    PROCESSING,
    FAILED,
    SYNCED
}

enum class EntitySyncState {
    SYNCED,
    DIRTY,
    PENDING_DELETE
}

enum class SyncSessionStatus {
    RUNNING,
    SUCCESS,
    FAILED,
    PARTIAL
}

enum class SyncTrigger {
    LOGIN,
    AFTER_MUTATION,
    MANUAL,
    WORKER_RETRY,
    WORKER
}

enum class OwnerType {
    TASK_DESCRIPTION,
    TASK_SOLUTION
}
