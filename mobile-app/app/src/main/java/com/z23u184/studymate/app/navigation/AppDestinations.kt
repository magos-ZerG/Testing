package com.z23u184.studymate.app.navigation

object AppDestinations {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val TOPICS = "topics"
    const val CREATE_TOPIC = "create_topic"
    const val TOPIC_TASKS = "topic_tasks/{topicId}"
    const val CREATE_TASK = "create_task/{topicId}"
    const val TASK_DETAILS = "task_details/{taskId}"
    const val FLASHCARDS = "flashcards/{topicId}"

    fun topicTasks(topicId: String) = "topic_tasks/$topicId"
    fun createTask(topicId: String) = "create_task/$topicId"
    fun taskDetails(taskId: String) = "task_details/$taskId"
    fun flashcards(topicId: String) = "flashcards/$topicId"
}
