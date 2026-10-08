package com.z23u184.studymate.domain.service

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
interface ClockProvider {
    fun now(): Instant
}
