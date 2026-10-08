package com.z23u184.studymate.app.ui.topic.topiclist.component

import androidx.compose.runtime.Composable
import com.z23u184.studymate.app.ui.common.component.EmptyContent
import com.z23u184.studymate.app.ui.topic.topiclist.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun TopicListEmpty() = EmptyContent(stringResource(R.string.no_topics))
