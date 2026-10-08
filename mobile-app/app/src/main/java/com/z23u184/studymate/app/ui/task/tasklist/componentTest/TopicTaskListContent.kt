package com.z23u184.studymate.app.ui.task.tasklist.componentTest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.ui.common.component.EmptyContent
import com.z23u184.studymate.app.ui.common.component.ErrorContent
import com.z23u184.studymate.app.ui.common.component.LoadingContent
import com.z23u184.studymate.app.ui.common.component.PrimaryButton
import com.z23u184.studymate.app.ui.task.tasklist.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun TopicTaskListContent(
    state: TopicTaskListScreenUiState,
    onTaskClick: (String) -> Unit,
    onDeleteTaskClick: (String) -> Unit,
    onFlashcardsClick: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        state.isLoading -> LoadingContent()
        state.errorMessage != null -> ErrorContent(state.errorMessage.asString(), onRetry)
        state.isEmpty -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TopicHeader(state.topicTitle)
            PrimaryButton(
                text = stringResource(R.string.flashcards_mode),
                onClick = onFlashcardsClick,
                enabled = false,
            )
            EmptyContent(stringResource(R.string.empty_topic_tasks))
        }
        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TopicHeader(state.topicTitle) }
            item {
                PrimaryButton(
                    text = stringResource(R.string.flashcards_mode),
                    onClick = onFlashcardsClick,
                    enabled = state.tasks.isNotEmpty(),
                )
            }
            items(state.tasks, key = { it.id }) { task ->
                TaskListItem(
                    item = task,
                    onClick = { onTaskClick(task.id) },
                    onDeleteClick = { onDeleteTaskClick(task.id) },
                )
            }
        }
    }
}
