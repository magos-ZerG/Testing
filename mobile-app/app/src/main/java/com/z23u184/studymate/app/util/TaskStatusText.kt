package com.z23u184.studymate.app.util

import androidx.annotation.StringRes
import com.z23u184.studymate.app.R
import com.z23u184.studymate.domain.model.TaskStatus

@StringRes
fun TaskStatus.titleResId(): Int = when (this) {
    TaskStatus.PLANNED -> R.string.status_planned
    TaskStatus.IN_PROGRESS -> R.string.status_in_progress
    TaskStatus.DONE -> R.string.status_done
    TaskStatus.ARCHIVED -> R.string.status_archived
}
