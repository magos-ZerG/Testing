package com.z23u184.studymate.app.ui.topic.topiclist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.z23u184.studymate.app.R
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.ui.common.component.ConfirmDialog
import com.z23u184.studymate.app.ui.common.component.LoadingContent
import com.z23u184.studymate.app.ui.common.component.MainInfoCard
import com.z23u184.studymate.app.ui.common.component.StudyMateScaffold
import com.z23u184.studymate.app.ui.settings.AppLanguage
import com.z23u184.studymate.app.ui.settings.LocalAppSettingsController
import com.z23u184.studymate.app.ui.topic.topiclist.component.TopicListContent
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun TopicListScreen(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    viewModel: TopicListScreenViewModel = koinViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    var topicIdPendingDelete by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.NavigateTo -> navController.navigate(event.route) { launchSingleTop = true }
                UiEvent.NavigateBack -> navController.popBackStack()
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message.asString())
                is UiEvent.OpenAttachment -> Unit
            }
        }
    }

    topicIdPendingDelete?.let { topicId ->
        ConfirmDialog(
            title = stringResource(R.string.delete_topic_question),
            text = stringResource(R.string.delete_topic_text),
            onConfirm = {
                topicIdPendingDelete = null
                viewModel.onConfirmDeleteTopic(topicId)
            },
            onDismiss = { topicIdPendingDelete = null },
        )
    }

    if (state.isLogoutInProgress) {
        StudyMateScaffold(
            snackbarHostState = snackbarHostState,
            title = stringResource(R.string.logout_title),
        ) {
            LoadingContent(message = stringResource(R.string.logout_progress))
        }
        return
    }

    TopicListMainScaffold(
        state = state,
        snackbarHostState = snackbarHostState,
        onLoginClick = viewModel::onLoginClick,
        onLogoutClick = viewModel::onLogoutClick,
        onCreateTopicClick = viewModel::onCreateTopicClick,
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            TopicListContent(
                state = state,
                onTopicClick = viewModel::onTopicClick,
                onDeleteTopicClick = { topicIdPendingDelete = it },
                onRetry = viewModel::load,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicListMainScaffold(
    state: TopicListScreenUiState,
    snackbarHostState: SnackbarHostState,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onCreateTopicClick: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            TopicListDrawerContent(
                state = state,
                onLoginClick = {
                    scope.launch { drawerState.close() }
                    onLoginClick()
                },
                onLogoutClick = {
                    scope.launch { drawerState.close() }
                    onLogoutClick()
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.topics)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.open_menu))
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            floatingActionButton = {
                FloatingActionButton(onClick = onCreateTopicClick) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_topic))
                }
            },
            content = content,
        )
    }
}

@Composable
private fun TopicListDrawerContent(
    state: TopicListScreenUiState,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val settingsController = LocalAppSettingsController.current
    val settings by settingsController.settings.collectAsStateWithLifecycle()

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.study_mate),
                style = MaterialTheme.typography.headlineSmall,
            )

            HorizontalDivider()

            if (state.isAuthorized) {
                MainInfoCard {
                    Text(
                        text = stringResource(R.string.account),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = state.userDisplayName ?: stringResource(R.string.email_not_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.logout_from_account))
                }
            } else {
                Text(
                    text = stringResource(R.string.local_mode_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.login))
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (settings.isDarkTheme) stringResource(R.string.theme_dark) else stringResource(R.string.theme_light),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.isDarkTheme,
                    onCheckedChange = settingsController::setDarkTheme,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
                LanguageOption(
                    title = stringResource(R.string.language_ru),
                    selected = settings.language == AppLanguage.RU,
                    onClick = { settingsController.setLanguage(AppLanguage.RU) },
                )
                LanguageOption(
                    title = stringResource(R.string.language_en),
                    selected = settings.language == AppLanguage.EN,
                    onClick = { settingsController.setLanguage(AppLanguage.EN) },
                )
            }
        }
    }
}

@Composable
private fun LanguageOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}
