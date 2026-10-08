package com.z23u184.studymate.app.ui.topic.createtopic

import com.z23u184.studymate.app.util.UiText

data class CreateTopicScreenUiState(
    val title: String = "",
    val isLoading: Boolean = false,
    val titleError: UiText? = null,
    val saveError: UiText? = null,
) {
    val isSaveEnabled: Boolean
        get() = title.isNotBlank()
}
