package com.z23u184.studymate.data.mapper

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
internal fun Instant.toEpochMillisValue(): Long = toEpochMilliseconds()

@OptIn(ExperimentalTime::class)
internal fun Long.toInstantValue(): Instant = Instant.fromEpochMilliseconds(this)
