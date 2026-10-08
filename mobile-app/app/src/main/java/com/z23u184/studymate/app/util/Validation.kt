package com.z23u184.studymate.app.util

fun isValidEmail(value: String): Boolean =
    value.contains('@') && value.contains('.') && value.length >= 5
