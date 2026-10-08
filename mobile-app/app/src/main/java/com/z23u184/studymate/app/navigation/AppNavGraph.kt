package com.z23u184.studymate.app.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.z23u184.studymate.app.ui.AppUiConfig
import com.z23u184.studymate.app.ui.auth.login.LoginScreen
import com.z23u184.studymate.app.ui.auth.register.RegisterScreen
import com.z23u184.studymate.app.ui.flashcards.FlashcardSessionScreen
import com.z23u184.studymate.app.ui.flashcards.FlashcardSessionTestScreen
import com.z23u184.studymate.app.ui.task.createtask.CreateTaskScreen
import com.z23u184.studymate.app.ui.task.taskdetails.TaskDetailsScreen
import com.z23u184.studymate.app.ui.task.tasklist.TopicTaskListScreen
import com.z23u184.studymate.app.ui.auth.login.LoginTestScreen
import com.z23u184.studymate.app.ui.auth.register.RegisterTestScreen
import com.z23u184.studymate.app.ui.task.createtask.CreateTaskTestScreen
import com.z23u184.studymate.app.ui.task.taskdetails.TaskDetailsTestScreen
import com.z23u184.studymate.app.ui.task.tasklist.TopicTaskListTestScreen
import com.z23u184.studymate.app.ui.topic.createtopic.CreateTopicTestScreen
import com.z23u184.studymate.app.ui.topic.topiclist.TopicListTestScreen
import com.z23u184.studymate.app.ui.topic.createtopic.CreateTopicScreen
import com.z23u184.studymate.app.ui.topic.topiclist.TopicListScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    startDestination: String,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(AppDestinations.LOGIN) {
            if (AppUiConfig.useTestUi) {
                LoginTestScreen(navController = navController, snackbarHostState = snackbarHostState)
            } else {
                LoginScreen(navController = navController, snackbarHostState = snackbarHostState)
            }
        }
        composable(AppDestinations.REGISTER) {
            if (AppUiConfig.useTestUi) {
                RegisterTestScreen(navController = navController, snackbarHostState = snackbarHostState)
            } else {
                RegisterScreen(navController = navController, snackbarHostState = snackbarHostState)
            }
        }
        composable(AppDestinations.TOPICS) {
            if (AppUiConfig.useTestUi) {
                TopicListTestScreen(navController = navController, snackbarHostState = snackbarHostState)
            } else {
                TopicListScreen(navController = navController, snackbarHostState = snackbarHostState)
            }
        }
        composable(AppDestinations.CREATE_TOPIC) {
            if (AppUiConfig.useTestUi) {
                CreateTopicTestScreen(navController = navController, snackbarHostState = snackbarHostState)
            } else {
                CreateTopicScreen(navController = navController, snackbarHostState = snackbarHostState)
            }
        }
        composable(
            route = AppDestinations.TOPIC_TASKS,
            arguments = listOf(navArgument(NavArguments.TOPIC_ID) { type = NavType.StringType }),
        ) {
            if (AppUiConfig.useTestUi) {
                TopicTaskListTestScreen(navController = navController, snackbarHostState = snackbarHostState)
            } else {
                TopicTaskListScreen(navController = navController, snackbarHostState = snackbarHostState)
            }
        }
        composable(
            route = AppDestinations.CREATE_TASK,
            arguments = listOf(navArgument(NavArguments.TOPIC_ID) { type = NavType.StringType }),
        ) {
            if (AppUiConfig.useTestUi) {
                CreateTaskTestScreen(navController = navController, snackbarHostState = snackbarHostState)
            } else {
                CreateTaskScreen(navController = navController, snackbarHostState = snackbarHostState)
            }
        }
        composable(
            route = AppDestinations.FLASHCARDS,
            arguments = listOf(navArgument(NavArguments.TOPIC_ID) { type = NavType.StringType }),
        ) {
            if (AppUiConfig.useTestUi) {
                FlashcardSessionTestScreen(navController = navController, snackbarHostState = snackbarHostState)
            } else {
                FlashcardSessionScreen(navController = navController, snackbarHostState = snackbarHostState)
            }
        }
        composable(
            route = AppDestinations.TASK_DETAILS,
            arguments = listOf(navArgument(NavArguments.TASK_ID) { type = NavType.StringType }),
        ) {
            if (AppUiConfig.useTestUi) {
                TaskDetailsTestScreen(navController = navController, snackbarHostState = snackbarHostState)
            } else {
                TaskDetailsScreen(navController = navController, snackbarHostState = snackbarHostState)
            }
        }
    }
}
