package com.z23u184.studymate.app.ui.flashcards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.z23u184.studymate.app.R
import com.z23u184.studymate.app.navigation.AppDestinations
import com.z23u184.studymate.app.navigation.popBackStackOrNavigate
import com.z23u184.studymate.app.ui.common.UiEvent
import com.z23u184.studymate.app.ui.common.component.EmptyContent
import com.z23u184.studymate.app.ui.common.component.ErrorContent
import com.z23u184.studymate.app.ui.common.component.LoadingContent
import com.z23u184.studymate.app.ui.common.component.PrimaryButton
import com.z23u184.studymate.app.ui.common.component.StudyMateScaffold
import org.koin.androidx.compose.koinViewModel

@Composable
fun FlashcardSessionScreen(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    viewModel: FlashcardSessionViewModel = koinViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                UiEvent.NavigateBack -> navController.popBackStackOrNavigate(AppDestinations.TOPICS)
                is UiEvent.NavigateTo -> navController.navigate(event.route) { launchSingleTop = true }
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message.asString())
                is UiEvent.OpenAttachment -> Unit
            }
        }
    }

    state.finishDialog?.let { dialog ->
        FlashcardFinishDialog(
            dialog = dialog,
            onDismiss = viewModel::onFinishDialogDismiss,
        )
    }

    StudyMateScaffold(
        snackbarHostState = snackbarHostState,
        title = stringResource(R.string.flashcards_title),
        onBackClick = viewModel::onBackClick,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            FlashcardSessionContent(
                state = state,
                onStartClick = viewModel::onStartClick,
                onCardClick = viewModel::onCardClick,
                onNextClick = viewModel::onNextClick,
                onRetry = viewModel::load,
            )
        }
    }
}

@Composable
private fun FlashcardSessionContent(
    state: FlashcardSessionUiState,
    onStartClick: () -> Unit,
    onCardClick: () -> Unit,
    onNextClick: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        state.isLoading -> LoadingContent()
        state.errorMessage != null -> ErrorContent(state.errorMessage.asString(), onRetry)
        state.cards.isEmpty() -> EmptyContent(stringResource(R.string.flashcards_empty))
        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.flashcards_timer, formatDuration(state.elapsedMs)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            if (state.previousBest != null) {
                Text(
                    text = stringResource(
                        R.string.flashcards_best_result,
                        state.previousBest.questionsCount,
                        formatDuration(state.previousBest.durationMs),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))

            val progress = state.progress
            if (state.isStarted && state.currentCard != null) {
                Text(
                    text = stringResource(R.string.flashcards_progress, progress.first, progress.second),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (!state.isStarted) {
                    PrimaryButton(
                        text = if (state.finishDialog == null) stringResource(R.string.flashcards_start) else stringResource(R.string.flashcards_restart),
                        onClick = onStartClick,
                    )
                } else {
                    FlashcardCard(
                        state = state,
                        onClick = onCardClick,
                    )
                }
            }

            if (state.isStarted && state.isAnswerVisible) {
                PrimaryButton(
                    text = stringResource(R.string.flashcards_next),
                    onClick = onNextClick,
                )
            }
        }
    }
}

@Composable
private fun FlashcardCard(
    state: FlashcardSessionUiState,
    onClick: () -> Unit,
) {
    val card = state.currentCard ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = card.question,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.isAnswerVisible) {
                Text(
                    text = stringResource(R.string.flashcards_answer),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = card.answer,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    text = stringResource(R.string.flashcards_show_answer_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun FlashcardFinishDialog(
    dialog: FlashcardFinishDialogUi,
    onDismiss: () -> Unit,
) {
    val previousBest = dialog.previousBest?.let {
        stringResource(R.string.flashcards_best_short, it.questionsCount, formatDuration(it.durationMs))
    } ?: stringResource(R.string.flashcards_no_previous_best)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.flashcards_finished_title)) },
        text = {
            Text(
                stringResource(
                    R.string.flashcards_finished_text,
                    dialog.questionsCount,
                    formatDuration(dialog.durationMs),
                    previousBest,
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (ms % 1000) / 100
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}
