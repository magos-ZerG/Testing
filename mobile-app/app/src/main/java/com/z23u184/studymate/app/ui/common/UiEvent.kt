package com.z23u184.studymate.app.ui.common

import android.net.Uri
import com.z23u184.studymate.app.util.UiText

sealed interface UiEvent {
    data class NavigateTo(val route: String) : UiEvent
    data object NavigateBack : UiEvent
    data class ShowSnackbar(val message: UiText) : UiEvent
    data class OpenAttachment(val uri: Uri, val mimeType: String) : UiEvent
}
