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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
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
fun FlashcardSessionTestScreen(
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
        FlashcardTestFinishDialog(
            dialog = dialog,
            onDismiss = viewModel::onFinishDialogDismiss,
        )
    }

    StudyMateScaffold(
        snackbarHostState = snackbarHostState,
        title = "Тест по карточкам",
        onBackClick = viewModel::onBackClick,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            FlashcardSessionTestContent(
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
private fun FlashcardSessionTestContent(
    state: FlashcardSessionUiState,
    onStartClick: () -> Unit,
    onCardClick: () -> Unit,
    onNextClick: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        state.isLoading -> LoadingContent(message = "Загрузка карточек...")
        state.errorMessage != null -> ErrorContent(state.errorMessage.asString(), onRetry)
        state.cards.isEmpty() -> EmptyContent("Для этой темы нет карточек")
        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Таймер: ${formatTestDuration(state.elapsedMs)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = state.previousBest?.let {
                    "Лучший результат: ${it.questionsCount} карточек за ${formatTestDuration(it.durationMs)}"
                } ?: "Лучший результат: пока нет",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            if (state.isStarted && state.currentCard != null) {
                Text(
                    text = "Карточка ${state.progress.first} из ${state.progress.second}",
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
                        text = if (state.finishDialog == null) "Начать" else "Начать заново",
                        onClick = onStartClick,
                    )
                } else {
                    FlashcardTestCard(
                        state = state,
                        onClick = onCardClick,
                    )
                }
            }

            if (state.isStarted && state.isAnswerVisible) {
                PrimaryButton(
                    text = "Далее",
                    onClick = onNextClick,
                )
            }
        }
    }
}

@Composable
private fun FlashcardTestCard(
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
                    text = "Ответ",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = card.answer,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    text = "Нажмите на карточку, чтобы показать ответ",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun FlashcardTestFinishDialog(
    dialog: FlashcardFinishDialogUi,
    onDismiss: () -> Unit,
) {
    val previousBest = dialog.previousBest?.let {
        "${it.questionsCount} карточек за ${formatTestDuration(it.durationMs)}"
    } ?: "пока нет"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Тест завершён") },
        text = {
            Text(
                "Пройдено карточек: ${dialog.questionsCount}. " +
                        "Время: ${formatTestDuration(dialog.durationMs)}. " +
                        "Предыдущий лучший результат: $previousBest."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ОК")
            }
        },
    )
}

private fun formatTestDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (ms % 1000) / 100
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}
