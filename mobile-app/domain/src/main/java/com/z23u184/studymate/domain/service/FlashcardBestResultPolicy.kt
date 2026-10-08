package com.z23u184.studymate.domain.service

import com.z23u184.studymate.domain.model.FlashcardBestResult

interface FlashcardBestResultPolicy {
    fun isBetter(candidate: FlashcardBestResult, current: FlashcardBestResult?): Boolean
}

class DefaultFlashcardBestResultPolicy : FlashcardBestResultPolicy {
    override fun isBetter(candidate: FlashcardBestResult, current: FlashcardBestResult?): Boolean {
        if (current == null) return true
        if (candidate.questionsCount != current.questionsCount) {
            return candidate.questionsCount > current.questionsCount
        }
        return candidate.durationMs < current.durationMs
    }
}
