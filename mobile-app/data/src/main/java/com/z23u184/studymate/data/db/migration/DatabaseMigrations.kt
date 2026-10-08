package com.z23u184.studymate.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS flashcard_best_results (
                id TEXT NOT NULL PRIMARY KEY,
                topicId TEXT NOT NULL,
                questionsCount INTEGER NOT NULL,
                durationMs INTEGER NOT NULL,
                completedAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL,
                remoteId TEXT,
                isDeleted INTEGER NOT NULL DEFAULT 0,
                syncState TEXT NOT NULL DEFAULT 'DIRTY',
                FOREIGN KEY(topicId) REFERENCES topics(id) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_flashcard_best_results_topicId ON flashcard_best_results(topicId)")
    }
}
