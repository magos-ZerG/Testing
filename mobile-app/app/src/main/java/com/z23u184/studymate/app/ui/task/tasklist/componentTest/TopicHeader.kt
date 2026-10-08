package com.z23u184.studymate.app.ui.task.tasklist.componentTest

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.z23u184.studymate.app.ui.task.tasklist.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun TopicHeader(topicTitle: String) {
    Column {
        Text(topicTitle)
        Text(stringResource(R.string.questions))
    }
}
