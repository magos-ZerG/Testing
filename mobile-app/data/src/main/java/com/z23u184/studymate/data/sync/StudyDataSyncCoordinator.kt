package com.z23u184.studymate.data.sync

import androidx.room.withTransaction
import com.z23u184.studymate.data.db.StudyMateDatabase
import com.z23u184.studymate.data.db.entity.AttachmentEntity
import com.z23u184.studymate.data.db.entity.FlashcardBestResultEntity
import com.z23u184.studymate.data.db.entity.EntitySyncState
import com.z23u184.studymate.data.db.entity.OwnerType
import com.z23u184.studymate.data.db.entity.SyncEntityType
import com.z23u184.studymate.data.db.entity.SyncOperation
import com.z23u184.studymate.data.db.entity.SyncQueueEntity
import com.z23u184.studymate.data.db.entity.SyncQueueStatus
import com.z23u184.studymate.data.db.entity.SyncSessionEntity
import com.z23u184.studymate.data.db.entity.SyncSessionStatus
import com.z23u184.studymate.data.db.entity.SyncTrigger
import com.z23u184.studymate.data.db.entity.TaskEntity
import com.z23u184.studymate.data.db.entity.TaskSolutionEntity
import com.z23u184.studymate.data.db.entity.TopicEntity
import com.z23u184.studymate.data.datasource.local.AttachmentLocalDataSource
import com.z23u184.studymate.data.datasource.local.FlashcardBestResultLocalDataSource
import com.z23u184.studymate.data.datasource.local.SyncQueueLocalDataSource
import com.z23u184.studymate.data.datasource.local.SyncSessionLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskLocalDataSource
import com.z23u184.studymate.data.datasource.local.TaskSolutionLocalDataSource
import com.z23u184.studymate.data.datasource.local.TopicLocalDataSource
import com.z23u184.studymate.data.network.dto.InitialSyncRequestDto
import com.z23u184.studymate.data.network.dto.PullChangesResponseDto
import com.z23u184.studymate.data.network.dto.PushChangesRequestDto
import com.z23u184.studymate.data.network.dto.PushResultItemDto
import com.z23u184.studymate.data.network.dto.RemoteAttachmentPullDto
import com.z23u184.studymate.data.network.dto.RemoteAttachmentPushDto
import com.z23u184.studymate.data.network.dto.RemoteFlashcardBestResultPullDto
import com.z23u184.studymate.data.network.dto.RemoteFlashcardBestResultPushDto
import com.z23u184.studymate.data.network.dto.RemoteTaskPullDto
import com.z23u184.studymate.data.network.dto.RemoteTaskPushDto
import com.z23u184.studymate.data.network.dto.RemoteTaskSolutionPullDto
import com.z23u184.studymate.data.network.dto.RemoteTaskSolutionPushDto
import com.z23u184.studymate.data.network.dto.RemoteTopicPullDto
import com.z23u184.studymate.data.network.dto.RemoteTopicPushDto
import com.z23u184.studymate.data.session.SessionLocalDataSource
import com.z23u184.studymate.data.sync.merge.MergeWinner
import com.z23u184.studymate.data.sync.merge.SyncConflictResolver
import com.z23u184.studymate.data.sync.remote.SyncRemoteDataSource
import com.z23u184.studymate.domain.model.AttachmentUploadState
import com.z23u184.studymate.domain.logging.StudyMateLogger
import java.io.File
import java.time.Instant
import java.time.format.DateTimeParseException

class StudyDataSyncCoordinator(
    private val database: StudyMateDatabase,
    private val topicLocalDataSource: TopicLocalDataSource,
    private val taskLocalDataSource: TaskLocalDataSource,
    private val taskSolutionLocalDataSource: TaskSolutionLocalDataSource,
    private val attachmentLocalDataSource: AttachmentLocalDataSource,
    private val flashcardBestResultLocalDataSource: FlashcardBestResultLocalDataSource,
    private val syncQueueLocalDataSource: SyncQueueLocalDataSource,
    private val syncSessionLocalDataSource: SyncSessionLocalDataSource,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val syncRemoteDataSource: SyncRemoteDataSource,
    private val syncConflictResolver: SyncConflictResolver,
) {
    suspend fun syncNow(trigger: SyncTrigger): SyncResult {
        val startedAt = System.currentTimeMillis()
        StudyMateLogger.i(TAG, "Sync started trigger=$trigger")
        val sessionId = syncSessionLocalDataSource.insert(
            SyncSessionEntity(startedAtEpochMs = startedAt, trigger = trigger)
        )

        return runCatching {
            val result = if (trigger == SyncTrigger.LOGIN) {
                handleInitialSyncAfterLogin()
            } else {
                val pushed = pushPendingChanges()
                val uploaded = uploadPendingAttachments()
                val pulled = pullRemoteChanges()
                SyncResult(
                    pushedCount = pushed + uploaded,
                    pulledCount = pulled,
                    failedCount = 0,
                    hadChanges = pushed > 0 || uploaded > 0 || pulled > 0,
                )
            }

            syncSessionLocalDataSource.finishSession(
                id = sessionId,
                finishedAtEpochMs = System.currentTimeMillis(),
                status = if (result.failedCount > 0) SyncSessionStatus.PARTIAL else SyncSessionStatus.SUCCESS,
                pushedCount = result.pushedCount,
                pulledCount = result.pulledCount,
                failedCount = result.failedCount,
                errorMessage = null,
            )

            StudyMateLogger.i(TAG, "Sync finished trigger=$trigger pushed=${result.pushedCount} pulled=${result.pulledCount} failed=${result.failedCount} hadChanges=${result.hadChanges}")
            result
        }.getOrElse { error ->
            StudyMateLogger.e(TAG, "Sync failed trigger=$trigger", error)
            syncSessionLocalDataSource.finishSession(
                id = sessionId,
                finishedAtEpochMs = System.currentTimeMillis(),
                status = SyncSessionStatus.FAILED,
                pushedCount = 0,
                pulledCount = 0,
                failedCount = 1,
                errorMessage = error.message,
            )
            SyncResult(0, 0, 1, false)
        }
    }

    private suspend fun handleInitialSyncAfterLogin(): SyncResult {
        val localPush = buildPendingPushRequest(
            syncQueueLocalDataSource.getProcessable(System.currentTimeMillis())
        )

        val response = syncRemoteDataSource.initialSync(
            InitialSyncRequestDto(
                push = localPush,
                pullSince = sessionLocalDataSource.getLastSyncAtEpochMs()?.toIsoString(),
            )
        )

        applyPushResults(response.push.topics, SyncEntityType.TOPIC)
        applyPushResults(response.push.tasks, SyncEntityType.TASK)
        applyPushResults(response.push.solutions, SyncEntityType.TASK_SOLUTION)
        applyPushResults(response.push.attachmentsMetadata, SyncEntityType.ATTACHMENT)
        applyPushResults(response.push.flashcardBestResults, SyncEntityType.FLASHCARD_BEST_RESULT)

        val uploaded = uploadPendingAttachments()
        val pulled = applyPullResponse(response.pull)

        val pushed =
            response.push.topics.size +
                    response.push.tasks.size +
                    response.push.solutions.size +
                    response.push.attachmentsMetadata.size +
                    response.push.flashcardBestResults.size +
                    uploaded

        return SyncResult(
            pushedCount = pushed,
            pulledCount = pulled,
            failedCount = 0,
            hadChanges = pushed > 0 || pulled > 0,
        )
    }

    private suspend fun pushPendingChanges(): Int {
        val now = System.currentTimeMillis()
        val processable = syncQueueLocalDataSource.getProcessable(now)
        StudyMateLogger.d(TAG, "Processable sync queue items=${processable.size}")
        var pushed = 0
        processable.forEach { item ->
            StudyMateLogger.d(TAG, "Pushing item id=${item.id} entityType=${item.entityType} entityId=${item.entityId} operation=${item.operation} attempt=${item.attemptCount}")
            syncQueueLocalDataSource.markProcessing(item.id, now)
            runCatching { tryPushItem(item) }
                .onSuccess { changed ->
                    if (changed) {
                        syncQueueLocalDataSource.markSynced(item.id, System.currentTimeMillis())
                        StudyMateLogger.d(TAG, "Push completed itemId=${item.id} entityType=${item.entityType}")
                        pushed++
                    } else {
                        StudyMateLogger.w(TAG, "Push did not complete itemId=${item.id} entityType=${item.entityType}")
                        val attemptCount = item.attemptCount + 1
                        val backoffMs = 1_000L * (1 shl attemptCount.coerceAtMost(6))
                        syncQueueLocalDataSource.markFailed(
                            id = item.id,
                            status = SyncQueueStatus.FAILED,
                            error = "Push did not complete",
                            attemptCount = attemptCount,
                            nextRetryAtEpochMs = System.currentTimeMillis() + backoffMs,
                            nowEpochMs = System.currentTimeMillis(),
                        )
                    }
                }
                .onFailure { error ->
                    StudyMateLogger.w(TAG, "Push failed itemId=${item.id} entityType=${item.entityType}", error)
                    val attemptCount = item.attemptCount + 1
                    val backoffMs = 1_000L * (1 shl attemptCount.coerceAtMost(6))
                    syncQueueLocalDataSource.markFailed(
                        id = item.id,
                        status = SyncQueueStatus.FAILED,
                        error = error.message ?: "Push failed",
                        attemptCount = attemptCount,
                        nextRetryAtEpochMs = System.currentTimeMillis() + backoffMs,
                        nowEpochMs = System.currentTimeMillis(),
                    )
                }
        }
        return pushed
    }

    private suspend fun uploadPendingAttachments(): Int {
        var uploaded = 0
        val pendingAttachments = attachmentLocalDataSource.getPendingFileUploads()
        StudyMateLogger.d(TAG, "Pending attachment uploads=${pendingAttachments.size}")

        pendingAttachments.forEach { entity ->
            if (entity.remoteId.isNullOrBlank()) return@forEach
            if (entity.localPath.isNullOrBlank()) return@forEach
            if (entity.uploadState == AttachmentUploadState.NOT_REQUIRED) return@forEach
            if (entity.uploadState == AttachmentUploadState.UPLOADED) return@forEach

            val changed = runCatching {
                uploadAttachmentFileIfNeeded(entity, entity.remoteId)
            }.getOrDefault(false)

            if (changed) uploaded++
        }

        return uploaded
    }

    private suspend fun tryPushItem(item: SyncQueueEntity): Boolean = when (item.entityType) {
        SyncEntityType.TOPIC -> pushTopic(item)
        SyncEntityType.TASK -> pushTask(item)
        SyncEntityType.TASK_SOLUTION -> pushSolution(item)
        SyncEntityType.ATTACHMENT -> pushAttachment(item)
        SyncEntityType.FLASHCARD_BEST_RESULT -> pushFlashcardBestResult(item)
    }

    private suspend fun pushTopic(item: SyncQueueEntity): Boolean {
        val entity = topicLocalDataSource.getTopic(item.entityId) ?: return true
        val response = syncRemoteDataSource.pushChanges(PushChangesRequestDto(topics = listOf(entity.toPushDto(item.operation))))
        val result = response.topics.firstOrNull() ?: return false
        database.topicDao().updateRemoteMetadata(
            id = entity.id,
            remoteId = result.remoteId,
            syncState = EntitySyncState.SYNCED,
            updatedAtEpochMs = result.serverUpdatedAt.toEpochMillisSafe(entity.updatedAtEpochMs),
        )
        return true
    }

    private suspend fun pushTask(item: SyncQueueEntity): Boolean {
        val entity = taskLocalDataSource.getTask(item.entityId) ?: return true
        val response = syncRemoteDataSource.pushChanges(PushChangesRequestDto(tasks = listOf(entity.toPushDto(item.operation))))
        val result = response.tasks.firstOrNull() ?: return false
        database.taskDao().updateRemoteMetadata(
            id = entity.id,
            remoteId = result.remoteId,
            syncState = EntitySyncState.SYNCED,
            updatedAtEpochMs = result.serverUpdatedAt.toEpochMillisSafe(entity.updatedAtEpochMs),
        )
        return true
    }

    private suspend fun pushSolution(item: SyncQueueEntity): Boolean {
        val entity = taskSolutionLocalDataSource.getById(item.entityId) ?: return true
        val response = syncRemoteDataSource.pushChanges(PushChangesRequestDto(solutions = listOf(entity.toPushDto(item.operation))))
        val result = response.solutions.firstOrNull() ?: return false
        database.taskSolutionDao().updateRemoteMetadata(
            id = entity.id,
            remoteId = result.remoteId,
            syncState = EntitySyncState.SYNCED,
            updatedAtEpochMs = result.serverUpdatedAt.toEpochMillisSafe(entity.updatedAtEpochMs),
        )
        return true
    }

    private suspend fun pushAttachment(item: SyncQueueEntity): Boolean {
        val entity = attachmentLocalDataSource.getById(item.entityId) ?: return true

        if (!entity.remoteId.isNullOrBlank() && entity.syncState == EntitySyncState.SYNCED) {
            return uploadAttachmentFileIfNeeded(entity, entity.remoteId)
        }

        val response = syncRemoteDataSource.pushChanges(
            PushChangesRequestDto(attachmentsMetadata = listOf(entity.toPushDto(item.operation)))
        )
        val result = response.attachmentsMetadata.firstOrNull() ?: return false
        val metadataUpdatedAt = result.serverUpdatedAt.toEpochMillisSafe(entity.updatedAtEpochMs)
        database.attachmentDao().updateRemoteFileInfo(
            id = entity.id,
            remoteId = result.remoteId,
            remoteFileId = entity.remoteFileId,
            uploadState = entity.uploadState,
            syncState = EntitySyncState.SYNCED,
            updatedAtEpochMs = metadataUpdatedAt,
        )

        return if (entity.localPath.isNullOrBlank() || entity.uploadState == AttachmentUploadState.NOT_REQUIRED || entity.uploadState == AttachmentUploadState.UPLOADED) {
            true
        } else {
            uploadAttachmentFileIfNeeded(
                entity.copy(remoteId = result.remoteId, updatedAtEpochMs = metadataUpdatedAt, syncState = EntitySyncState.SYNCED),
                result.remoteId,
            )
        }
    }

    private suspend fun pushFlashcardBestResult(item: SyncQueueEntity): Boolean {
        val entity = flashcardBestResultLocalDataSource.getById(item.entityId) ?: return true
        val response = syncRemoteDataSource.pushChanges(
            PushChangesRequestDto(flashcardBestResults = listOf(entity.toPushDto(item.operation)))
        )
        val result = response.flashcardBestResults.firstOrNull() ?: return false
        database.flashcardBestResultDao().updateRemoteMetadata(
            id = entity.id,
            remoteId = result.remoteId.takeIf { it.isNotBlank() } ?: entity.remoteId,
            syncState = EntitySyncState.SYNCED,
            updatedAtEpochMs = result.serverUpdatedAt.toEpochMillisSafe(entity.updatedAtEpochMs),
        )
        return true
    }

    private suspend fun uploadAttachmentFileIfNeeded(entity: AttachmentEntity, remoteAttachmentId: String): Boolean {
        val localPath = entity.localPath
        if (entity.uploadState == AttachmentUploadState.UPLOADED) return true
        if (entity.uploadState == AttachmentUploadState.NOT_REQUIRED) return true
        if (localPath.isNullOrBlank()) {
            StudyMateLogger.w(TAG, "Attachment upload failed: local path is missing attachmentId=${entity.id}")
            attachmentLocalDataSource.updateUploadState(entity.id, AttachmentUploadState.FAILED, System.currentTimeMillis())
            throw IllegalStateException("Attachment file path is missing for ${entity.id}")
        }

        val file = File(localPath)
        if (!file.exists() || !file.isFile) {
            StudyMateLogger.w(TAG, "Attachment upload failed: file is missing attachmentId=${entity.id} path=$localPath")
            attachmentLocalDataSource.updateUploadState(entity.id, AttachmentUploadState.FAILED, System.currentTimeMillis())
            throw IllegalStateException("Attachment file is missing for ${entity.id}")
        }

        attachmentLocalDataSource.updateUploadState(entity.id, AttachmentUploadState.UPLOADING, System.currentTimeMillis())
        val uploadResponse = syncRemoteDataSource.uploadAttachment(
            attachmentId = remoteAttachmentId,
            file = file,
            mimeType = entity.mimeType,
        )
        database.attachmentDao().updateRemoteFileInfo(
            id = entity.id,
            remoteId = remoteAttachmentId,
            remoteFileId = uploadResponse.remoteFileId,
            uploadState = uploadResponse.uploadState.toLocalUploadState(
                default = AttachmentUploadState.UPLOADED
            ),
            syncState = EntitySyncState.SYNCED,
            updatedAtEpochMs = uploadResponse.updatedAt.toEpochMillisSafe(System.currentTimeMillis()),
        )
        return true
    }

    private suspend fun pullRemoteChanges(): Int {
        val remote = syncRemoteDataSource.pullChanges(sessionLocalDataSource.getLastSyncAtEpochMs()?.toIsoString())
        return applyPullResponse(remote)
    }

    private suspend fun applyPullResponse(remote: PullChangesResponseDto): Int {
        var pulledCount = 0
        database.withTransaction {
            remote.topics.forEach { remoteTopic ->
                val mapped = remoteTopic.toEntity()
                val local = topicLocalDataSource.getTopic(mapped.id)
                if (syncConflictResolver.chooseTopic(local, mapped) == MergeWinner.REMOTE) {
                    topicLocalDataSource.saveTopic(mapped)
                    pulledCount++
                }
            }
            remote.tasks.forEach { remoteTask ->
                val mapped = remoteTask.toEntity()
                val local = taskLocalDataSource.getTask(mapped.id)
                if (syncConflictResolver.chooseTask(local, mapped) == MergeWinner.REMOTE) {
                    taskLocalDataSource.saveTask(mapped)
                    pulledCount++
                }
            }
            remote.solutions.forEach { remoteSolution ->
                val mapped = remoteSolution.toEntity()
                val local = taskSolutionLocalDataSource.getById(mapped.id)
                if (syncConflictResolver.chooseSolution(local, mapped) == MergeWinner.REMOTE) {
                    taskSolutionLocalDataSource.save(mapped)
                    pulledCount++
                }
            }
            remote.attachmentsMetadata.forEach { remoteAttachment ->
                val mapped = remoteAttachment.toEntity()
                val local = attachmentLocalDataSource.getById(mapped.id)
                if (syncConflictResolver.chooseAttachment(local, mapped) == MergeWinner.REMOTE) {
                    attachmentLocalDataSource.save(mapped)
                    pulledCount++
                }
            }
            remote.flashcardBestResults.forEach { remoteResult ->
                val mapped = remoteResult.toEntity()
                val local = flashcardBestResultLocalDataSource.getByTopicId(mapped.topicId)
                    ?: flashcardBestResultLocalDataSource.getById(mapped.id)
                if (syncConflictResolver.chooseFlashcardBestResult(local, mapped) == MergeWinner.REMOTE) {
                    flashcardBestResultLocalDataSource.save(mapped)
                    pulledCount++
                }
            }

            sessionLocalDataSource.setLastSyncAtEpochMs(
                remote.nextSince.toEpochMillisSafe()
            )
        }
        StudyMateLogger.d(TAG, "Pull applied count=$pulledCount")
        return pulledCount
    }

    private suspend fun buildPendingPushRequest(items: List<SyncQueueEntity>): PushChangesRequestDto {
        val topics = mutableListOf<RemoteTopicPushDto>()
        val tasks = mutableListOf<RemoteTaskPushDto>()
        val solutions = mutableListOf<RemoteTaskSolutionPushDto>()
        val attachments = mutableListOf<RemoteAttachmentPushDto>()
        val flashcardBestResults = mutableListOf<RemoteFlashcardBestResultPushDto>()

        items.forEach { item ->
            when (item.entityType) {
                SyncEntityType.TOPIC -> topicLocalDataSource.getTopic(item.entityId)?.let { topics += it.toPushDto(item.operation) }
                SyncEntityType.TASK -> taskLocalDataSource.getTask(item.entityId)?.let { tasks += it.toPushDto(item.operation) }
                SyncEntityType.TASK_SOLUTION -> taskSolutionLocalDataSource.getById(item.entityId)?.let { solutions += it.toPushDto(item.operation) }
                SyncEntityType.ATTACHMENT -> attachmentLocalDataSource.getById(item.entityId)?.let { attachments += it.toPushDto(item.operation) }
                SyncEntityType.FLASHCARD_BEST_RESULT -> flashcardBestResultLocalDataSource.getById(item.entityId)?.let { flashcardBestResults += it.toPushDto(item.operation) }
            }
        }

        return PushChangesRequestDto(topics = topics, tasks = tasks, solutions = solutions, attachmentsMetadata = attachments, flashcardBestResults = flashcardBestResults)
    }

    private suspend fun applyPushResults(results: List<PushResultItemDto>, entityType: SyncEntityType) {
        val now = System.currentTimeMillis()
        StudyMateLogger.d(TAG, "Applying push results entityType=$entityType count=${results.size}")
        results.forEach { result ->
            when (entityType) {
                SyncEntityType.TOPIC -> database.topicDao().updateRemoteMetadata(result.clientId, result.remoteId, EntitySyncState.SYNCED, result.serverUpdatedAt.toEpochMillisSafe(now))
                SyncEntityType.TASK -> database.taskDao().updateRemoteMetadata(result.clientId, result.remoteId, EntitySyncState.SYNCED, result.serverUpdatedAt.toEpochMillisSafe(now))
                SyncEntityType.TASK_SOLUTION -> {
                    val solution = taskSolutionLocalDataSource.getById(result.clientId)
                    if (solution != null) {
                        database.taskSolutionDao().updateRemoteMetadata(solution.id, result.remoteId, EntitySyncState.SYNCED, result.serverUpdatedAt.toEpochMillisSafe(now))
                    }
                }
                SyncEntityType.ATTACHMENT -> {
                    val current = attachmentLocalDataSource.getById(result.clientId)
                    database.attachmentDao().updateRemoteFileInfo(
                        id = result.clientId,
                        remoteId = result.remoteId,
                        remoteFileId = current?.remoteFileId,
                        uploadState = current?.uploadState ?: AttachmentUploadState.NOT_REQUIRED,
                        syncState = EntitySyncState.SYNCED,
                        updatedAtEpochMs = result.serverUpdatedAt.toEpochMillisSafe(now),
                    )
                }
                SyncEntityType.FLASHCARD_BEST_RESULT -> database.flashcardBestResultDao().updateRemoteMetadata(
                    result.clientId,
                    result.remoteId.takeIf { it.isNotBlank() },
                    EntitySyncState.SYNCED,
                    result.serverUpdatedAt.toEpochMillisSafe(now),
                )
            }
            syncQueueLocalDataSource.findSimilar(entityType, result.clientId)
                .filter { it.status != SyncQueueStatus.SYNCED }
                .forEach { syncQueueLocalDataSource.markSynced(it.id, now) }
        }
    }

    private companion object {
        const val TAG = "StudyDataSyncCoordinator"
    }
}

private fun TopicEntity.toPushDto(operation: SyncOperation) = RemoteTopicPushDto(
    clientId = id,
    updatedAt = updatedAtEpochMs.toIsoString(),
    isDeleted = isDeleted,
    operation = operation.name,
    title = if (isDeleted) null else title,
)

private fun TaskEntity.toPushDto(operation: SyncOperation) = RemoteTaskPushDto(
    clientId = id,
    updatedAt = updatedAtEpochMs.toIsoString(),
    isDeleted = isDeleted,
    operation = operation.name,
    topicClientId = topicId,
    topicRemoteId = null,
    title = if (isDeleted) null else title,
    description = if (isDeleted) null else description,
    status = status,
    deadlineAt = deadlineAtEpochMs?.toIsoString(),
)

private fun TaskSolutionEntity.toPushDto(operation: SyncOperation) = RemoteTaskSolutionPushDto(
    clientId = id,
    updatedAt = updatedAtEpochMs.toIsoString(),
    isDeleted = isDeleted,
    operation = operation.name,
    taskClientId = taskId,
    taskRemoteId = null,
    content = if (isDeleted) null else content,
)

private fun AttachmentEntity.toPushDto(operation: SyncOperation) = RemoteAttachmentPushDto(
    clientId = id,
    updatedAt = updatedAtEpochMs.toIsoString(),
    isDeleted = isDeleted,
    operation = operation.name,
    ownerType = ownerType.name,
    ownerTaskClientId = ownerTaskId,
    ownerTaskRemoteId = null,
    fileName = if (isDeleted) null else fileName,
    mimeType = if (isDeleted) null else mimeType,
    sizeBytes = if (isDeleted) null else sizeBytes,
    remoteFileId = remoteFileId,
    uploadState = uploadState.toServerUploadState(),
)

private fun FlashcardBestResultEntity.toPushDto(operation: SyncOperation) = RemoteFlashcardBestResultPushDto(
    clientId = id,
    updatedAt = updatedAtEpochMs.toIsoString(),
    isDeleted = isDeleted,
    operation = operation.name,
    topicClientId = topicId,
    topicRemoteId = null,
    questionsCount = if (isDeleted) null else questionsCount,
    durationMs = if (isDeleted) null else durationMs,
    completedAt = if (isDeleted) null else completedAtEpochMs.toIsoString(),
)

private fun RemoteTopicPullDto.toEntity() = TopicEntity(
    id = clientId,
    title = title,
    createdAtEpochMs = createdAt.toEpochMillisSafe(),
    updatedAtEpochMs = updatedAt.toEpochMillisSafe(),
    remoteId = remoteId,
    isDeleted = isDeleted,
    syncState = EntitySyncState.SYNCED,
)

private fun RemoteTaskPullDto.toEntity() = TaskEntity(
    id = clientId,
    topicId = topicClientId,
    title = title,
    description = description.orEmpty(),
    status = status,
    createdAtEpochMs = createdAt.toEpochMillisSafe(),
    updatedAtEpochMs = updatedAt.toEpochMillisSafe(),
    deadlineAtEpochMs = deadlineAt?.toEpochMillisSafe(),
    remoteId = remoteId,
    isDeleted = isDeleted,
    syncState = EntitySyncState.SYNCED,
)

private fun RemoteTaskSolutionPullDto.toEntity() = TaskSolutionEntity(
    id = clientId,
    taskId = taskClientId,
    content = content.orEmpty(),
    updatedAtEpochMs = updatedAt.toEpochMillisSafe(),
    remoteId = remoteId,
    isDeleted = isDeleted,
    syncState = EntitySyncState.SYNCED,
)

private fun RemoteAttachmentPullDto.toEntity() = AttachmentEntity(
    id = clientId,
    ownerType = runCatching { OwnerType.valueOf(ownerType) }.getOrDefault(OwnerType.TASK_DESCRIPTION),
    ownerTaskId = ownerTaskClientId,
    fileName = fileName,
    mimeType = mimeType.orEmpty(),
    sizeBytes = sizeBytes ?: 0L,
    createdAtEpochMs = createdAt.toEpochMillisSafe(),
    updatedAtEpochMs = updatedAt.toEpochMillisSafe(),
    localPath = null,
    remoteFileId = remoteFileId,
    uploadState = uploadState.toLocalUploadState(),
    remoteId = remoteId,
    isDeleted = isDeleted,
    syncState = EntitySyncState.SYNCED,
)

private fun RemoteFlashcardBestResultPullDto.toEntity() = FlashcardBestResultEntity(
    id = clientId,
    topicId = topicClientId,
    questionsCount = questionsCount,
    durationMs = durationMs,
    completedAtEpochMs = completedAt.toEpochMillisSafe(),
    updatedAtEpochMs = updatedAt.toEpochMillisSafe(),
    remoteId = remoteId,
    isDeleted = isDeleted,
    syncState = EntitySyncState.SYNCED,
)

private fun AttachmentUploadState.toServerUploadState(): String? = when (this) {
    AttachmentUploadState.NOT_REQUIRED -> null
    AttachmentUploadState.PENDING_UPLOAD -> "PENDING"
    AttachmentUploadState.UPLOADING -> "PENDING"
    AttachmentUploadState.UPLOADED -> "UPLOADED"
    AttachmentUploadState.FAILED -> "FAILED"
}

private fun String?.toLocalUploadState(default: AttachmentUploadState = AttachmentUploadState.NOT_REQUIRED): AttachmentUploadState {
    return when (this) {
        null -> default
        "PENDING" -> AttachmentUploadState.PENDING_UPLOAD
        "UPLOADED" -> AttachmentUploadState.UPLOADED
        "FAILED" -> AttachmentUploadState.FAILED
        "NOT_REQUIRED" -> AttachmentUploadState.NOT_REQUIRED
        "PENDING_UPLOAD" -> AttachmentUploadState.PENDING_UPLOAD
        "UPLOADING" -> AttachmentUploadState.PENDING_UPLOAD

        else -> default
    }
}

private fun Long.toIsoString(): String = Instant.ofEpochMilli(this).toString()
private fun Long?.toIsoString(): String? = this?.let { Instant.ofEpochMilli(it).toString() }
private fun String.toEpochMillisSafe(default: Long = System.currentTimeMillis()): Long =
    try { Instant.parse(this).toEpochMilli() } catch (_: DateTimeParseException) { default }
