package com.z23u184.studymate.app.util

import android.content.Context
import androidx.annotation.StringRes

sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText()

    fun asString(): String = when (this) {
        is DynamicString -> value
        is StringResource -> UiTextStringProvider.context?.getString(resId, *args.toTypedArray()).orEmpty()
    }

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> context.getString(resId, *args.toTypedArray())
    }
}

object UiTextStringProvider {
    var context: Context? = null
}
