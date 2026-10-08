package com.z23u184.studymate.app.ui.topic.createtopic.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.ui.common.component.FormTextField
import com.z23u184.studymate.app.ui.topic.createtopic.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun CreateTopicForm(
    state: CreateTopicScreenUiState,
    onTitleChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormTextField(state.title, onTitleChanged, stringResource(R.string.topic_title), state.titleError?.asString())
        state.saveError?.let { Text(it.asString()) }
    }
}
