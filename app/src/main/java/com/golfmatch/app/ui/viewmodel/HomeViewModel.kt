package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.RoundEvent
import com.golfmatch.app.domain.usecase.GetRoundEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ホーム（ラウンド募集一覧）画面のUiState（`D:\勉強\golf\UiState定義.md` 1章、技術設計書7章） */
data class HomeUiState(
    val isLoading: Boolean = false,
    val roundEvents: List<RoundEvent> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRoundEventsUseCase: GetRoundEventsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRoundEvents()
    }

    fun loadRoundEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { getRoundEventsUseCase() }
                .onSuccess { events ->
                    _uiState.value = _uiState.value.copy(isLoading = false, roundEvents = events)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "ラウンド募集一覧の取得に失敗しました"
                    )
                }
        }
    }
}
