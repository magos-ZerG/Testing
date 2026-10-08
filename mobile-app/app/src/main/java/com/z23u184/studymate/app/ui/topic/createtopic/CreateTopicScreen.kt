package com.z23u184.studymate.app.ui.topic.createtopic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.z23u184.studymate.app.navigation.AppDestinations
import com.z23u184.studymate.app.navigation.popBackStackOrNavigate
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.ui.common.component.MainInfoCard
import com.z23u184.studymate.app.ui.common.component.PrimaryButton
import com.z23u184.studymate.app.ui.common.component.StudyMateScaffold
import com.z23u184.studymate.app.ui.topic.createtopic.component.*
import org.koin.androidx.compose.koinViewModel

import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
import kotlinx.coroutines.launch

@Composable
fun CreateTopicScreen(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    viewModel: CreateTopicScreenViewModel = koinViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.NavigateTo -> navController.navigate(event.route)
                UiEvent.NavigateBack -> navController.popBackStackOrNavigate(AppDestinations.TOPICS)
                is UiEvent.ShowSnackbar -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(event.message.asString())
                    }
                }
                is UiEvent.OpenAttachment -> Unit
            }
        }
    }

    StudyMateScaffold(snackbarHostState = snackbarHostState, title = stringResource(R.string.create_topic_title), onBackClick = viewModel::onBackClick) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MainInfoCard {
                Text(stringResource(R.string.new_topic), style = MaterialTheme.typography.headlineSmall)
                CreateTopicForm(state, viewModel::onTitleChanged)
                PrimaryButton(stringResource(R.string.save), enabled = state.isSaveEnabled, loading = state.isLoading, onClick = viewModel::onSaveClick)
            }
        }
    }
}
