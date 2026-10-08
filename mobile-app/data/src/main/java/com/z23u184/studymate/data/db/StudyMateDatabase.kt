package com.z23u184.studymate.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.z23u184.studymate.data.db.converter.RoomEnumConverters
import com.z23u184.studymate.data.db.dao.AttachmentDao
import com.z23u184.studymate.data.db.dao.FlashcardBestResultDao
import com.z23u184.studymate.data.db.dao.SyncQueueDao
import com.z23u184.studymate.data.db.dao.SyncSessionDao
import com.z23u184.studymate.data.db.dao.TaskDao
import com.z23u184.studymate.data.db.dao.TaskSolutionDao
import com.z23u184.studymate.data.db.dao.TopicDao
import com.z23u184.studymate.data.db.entity.AttachmentEntity
import com.z23u184.studymate.data.db.entity.FlashcardBestResultEntity
import com.z23u184.studymate.data.db.entity.SyncQueueEntity
import com.z23u184.studymate.data.db.entity.SyncSessionEntity
import com.z23u184.studymate.data.db.entity.TaskEntity
import com.z23u184.studymate.data.db.entity.TaskSolutionEntity
import com.z23u184.studymate.data.db.entity.TopicEntity

@Database(
    entities = [
        TopicEntity::class,
        TaskEntity::class,
        TaskSolutionEntity::class,
        AttachmentEntity::class,
        FlashcardBestResultEntity::class,
        SyncQueueEntity::class,
        SyncSessionEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(RoomEnumConverters::class)
abstract class StudyMateDatabase : RoomDatabase() {
    abstract fun topicDao(): TopicDao
    abstract fun taskDao(): TaskDao
    abstract fun taskSolutionDao(): TaskSolutionDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun flashcardBestResultDao(): FlashcardBestResultDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncSessionDao(): SyncSessionDao
}
