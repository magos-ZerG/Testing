package com.z23u184.studymate.domain.repository

import com.z23u184.studymate.domain.model.Flashcard
import com.z23u184.studymate.domain.model.FlashcardBestResult
import com.z23u184.studymate.domain.model.TopicId

interface FlashcardRepository {
    suspend fun getCardsByTopic(topicId: TopicId): List<Flashcard>
    suspend fun getBestResult(topicId: TopicId): FlashcardBestResult?
    suspend fun saveBestResultIfBetter(result: FlashcardBestResult): FlashcardBestResult
}
