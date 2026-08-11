package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.usecase.CreateRoundEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import javax.inject.Inject

/** ラウンド新規作成画面のUiState（`D:\勉強\golf\UiState定義.md` 2章、技術設計書7章） */
data class CreateRoundUiState(
    val clubName: String = "",
    val dateTime: String = "",
    val fee: String = "",
    val capacity: String = "",
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ラウンド新規作成画面のViewModel（技術設計書3-1章・5-1章、`D:\勉強\golf\UiState定義.md` 2章）。
 *
 * [uiState.dateTime] はISOローカル日時形式（例: `2026-09-01T08:00`）での入力を前提とする。
 * 技術設計書5-1章のRoundEventにはエリア・目的タグ等の項目は定義されていないため、
 * [CreateRoundEventUseCase] の引数（倶楽部名・日時・費用・募集人数）のみを入力対象とする。
 */
@HiltViewModel
class CreateRoundViewModel @Inject constructor(
    private val createRoundEventUseCase: CreateRoundEventUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateRoundUiState())
    val uiState: StateFlow<CreateRoundUiState> = _uiState.asStateFlow()

    fun onClubNameChange(value: String) {
        _uiState.value = _uiState.value.copy(clubName = value, errorMessage = null)
    }

    fun onDateTimeChange(value: String) {
        _uiState.value = _uiState.value.copy(dateTime = value, errorMessage = null)
    }

    fun onFeeChange(value: String) {
        _uiState.value = _uiState.value.copy(fee = value, errorMessage = null)
    }

    fun onCapacityChange(value: String) {
        _uiState.value = _uiState.value.copy(capacity = value, errorMessage = null)
    }

    fun submit() {
        val state = _uiState.value
        val clubName = state.clubName.trim()
        val fee = state.fee.toIntOrNull()
        val capacity = state.capacity.toIntOrNull()
        val datetime = runCatching {
            LocalDateTime.parse(state.dateTime.trim()).toInstant(TimeZone.currentSystemDefault())
        }.getOrNull()

        if (clubName.isEmpty() || datetime == null || fee == null || capacity == null) {
            _uiState.value = state.copy(errorMessage = "入力内容を確認してください")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching { createRoundEventUseCase(clubName, datetime, fee, capacity) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, submitSuccess = true)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "ラウンド募集の作成に失敗しました"
                    )
                }
        }
    }
}
