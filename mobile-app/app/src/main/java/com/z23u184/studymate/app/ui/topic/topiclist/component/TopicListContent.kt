package com.z23u184.studymate.app.ui.topic.topiclist.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.z23u184.studymate.app.ui.topic.topiclist.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicListContent(
    state: TopicListScreenUiState,
    onTopicClick: (String) -> Unit,
    onDeleteTopicClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredTopics = state.topics.filter { topic ->
        topic.title.contains(searchQuery, ignoreCase = true)
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
            MainSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = stringResource(R.string.topic_search_placeholder),
            )

            when {
                state.isEmpty -> TopicListEmpty()
                filteredTopics.isEmpty() -> EmptyContent(stringResource(R.string.topics_not_found))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filteredTopics, key = { it.id }) { item ->
                        TopicListItem(
                            item = item,
                            onClick = { onTopicClick(item.id) },
                            onDeleteClick = { onDeleteTopicClick(item.id) },
                        )
                    }
                }
            }
        }
    }
}
