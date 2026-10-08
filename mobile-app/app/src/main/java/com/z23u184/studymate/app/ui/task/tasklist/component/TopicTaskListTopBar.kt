package com.z23u184.studymate.app.ui.task.tasklist.component

import androidx.compose.runtime.Composable
import com.z23u184.studymate.app.ui.common.component.StudyMateTopBar
import com.z23u184.studymate.app.ui.task.tasklist.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun TopicTaskListTopBar(topicTitle: String, onBackClick: () -> Unit) {
    StudyMateTopBar(title = topicTitle.ifBlank { stringResource(R.string.topic_questions) }, onBackClick = onBackClick)
}
