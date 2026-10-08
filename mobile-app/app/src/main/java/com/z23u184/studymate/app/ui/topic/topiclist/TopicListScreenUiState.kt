package com.z23u184.studymate.app.ui.topic.topiclist

import com.z23u184.studymate.app.util.UiText

data class TopicListScreenUiState(
    val isLoading: Boolean = true,
    val topics: List<TopicListItemUi> = emptyList(),
    val errorMessage: UiText? = null,
    val userDisplayName: String? = null,
    val isAuthorized: Boolean = false,
    val isLogoutInProgress: Boolean = false,
) {
    val isEmpty: Boolean
        get() = topics.isEmpty()
}

data class TopicListItemUi(
    val id: String,
    val title: String,
    val tasksCount: Int? = null,
    val updatedAtText: String? = null,
)
