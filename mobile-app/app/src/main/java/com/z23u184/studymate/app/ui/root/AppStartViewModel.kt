package com.z23u184.studymate.app.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z23u184.studymate.app.navigation.AppDestinations
import com.z23u184.studymate.domain.model.UserMode
import com.z23u184.studymate.domain.usecase.GetUserModeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppStartViewModel(
    private val getUserModeUseCase: GetUserModeUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppStartUiState())
    val uiState: StateFlow<AppStartUiState> = _uiState.asStateFlow()

    init {
        resolveStartDestination()
    }

    private fun resolveStartDestination() {
        viewModelScope.launch {
            val destination = runCatching {
                when (getUserModeUseCase()) {
                    UserMode.AUTHORIZED -> AppDestinations.TOPICS
                    UserMode.LOCAL -> AppDestinations.TOPICS
                }
            }.getOrDefault(AppDestinations.LOGIN)

            _uiState.value = AppStartUiState(
                isReady = true,
                startDestination = destination,
            )
        }
    }
}
