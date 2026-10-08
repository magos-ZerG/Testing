package com.z23u184.studymate.app.ui.topic.topiclist.componentTest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.ui.common.component.EmptyContent
import com.z23u184.studymate.app.ui.common.component.ErrorContent
import com.z23u184.studymate.app.ui.common.component.LoadingContent
import com.z23u184.studymate.app.ui.topic.topiclist.*

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
@Composable
fun TopicListContent(
    state: TopicListScreenUiState,
    onTopicClick: (String) -> Unit,
    onDeleteTopicClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        state.isLoading -> LoadingContent()
        state.errorMessage != null -> ErrorContent(state.errorMessage.asString(), onRetry)
        state.isEmpty -> TopicListEmpty()
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(if (state.isAuthorized) stringResource(R.string.authorized_mode) else stringResource(R.string.local_mode))
            }
            items(state.topics, key = { it.id }) { item ->
                TopicListItem(
                    item = item,
                    onClick = { onTopicClick(item.id) },
                    onDeleteClick = { onDeleteTopicClick(item.id) },
                )
            }
        }
    }
}
