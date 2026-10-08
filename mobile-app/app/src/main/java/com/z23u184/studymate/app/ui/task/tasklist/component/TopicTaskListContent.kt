package com.z23u184.studymate.app.ui.task.tasklist.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.ui.common.component.EmptyContent
import com.z23u184.studymate.app.ui.common.component.ErrorContent
import com.z23u184.studymate.app.ui.common.component.LoadingContent
import com.z23u184.studymate.app.ui.common.component.MainSearchField
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
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredTasks = state.tasks.filter { task ->
        task.title.contains(searchQuery, ignoreCase = true)
    }

    when {
        state.isLoading -> LoadingContent()
        state.errorMessage != null -> ErrorContent(state.errorMessage.asString(), onRetry)
        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PrimaryButton(
                text = stringResource(R.string.flashcards_mode),
                onClick = onFlashcardsClick,
                enabled = state.tasks.isNotEmpty(),
            )
            MainSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.question_search_placeholder),
            )
            when {
                state.isEmpty -> EmptyContent(stringResource(R.string.empty_topic_tasks))
                filteredTasks.isEmpty() -> EmptyContent(stringResource(R.string.tasks_not_found))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskListItem(
                            item = task,
                            onClick = { onTaskClick(task.id) },
                            onDeleteClick = { onDeleteTaskClick(task.id) },
                        )
                    }
                }
            }
        }
    }
}
