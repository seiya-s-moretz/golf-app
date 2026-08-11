package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.data.auth.AuthSessionManager
import com.golfmatch.app.domain.model.RoundEvent
import com.golfmatch.app.domain.usecase.ApplyRoundJoinUseCase
import com.golfmatch.app.domain.usecase.GetRoundEventUseCase
import com.golfmatch.app.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ラウンド詳細画面のUiState。
 *
 * 技術設計書7章にはラウンド詳細画面（既存4画面の一部）のUiState定義が明記されていないため、
 * 他画面のパターン（[HomeUiState]の一覧・[MyPageUiState]の`isSaving`/`errorMessage`等）に倣い
 * 最小限の構成で定義した（実装判断）。
 *
 * [isOrganizer] は自分が募集の主催者（[RoundEvent.createdBy]）かどうかを表し、
 * `true`の場合のみ参加申請一覧画面への導線を表示する（ADR-0001）。
 */
data class RoundDetailUiState(
    val isLoading: Boolean = false,
    val roundEvent: RoundEvent? = null,
    val isOrganizer: Boolean = false,
    val isApplying: Boolean = false,
    val applySuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ラウンド詳細画面のViewModel（技術設計書3-1章・6-4章、ADR-0001）。
 *
 * 参加申請は[ApplyRoundJoinUseCase]で行う（この時点では[RoundEvent.current]は加算されない）。
 * 主催者判定は現在ログイン中のユーザーID（[AuthSessionManager.currentUserId]）と
 * [RoundEvent.createdBy]の一致で行う（[MyPageViewModel]と同じ現在ユーザーID取得パターン）。
 */
@HiltViewModel
class RoundDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRoundEventUseCase: GetRoundEventUseCase,
    private val applyRoundJoinUseCase: ApplyRoundJoinUseCase,
    private val authSessionManager: AuthSessionManager
) : ViewModel() {

    private val eventId: String = checkNotNull(savedStateHandle.get<String>(Route.RoundDetail.ARG_EVENT_ID)) {
        "RoundDetailScreen requires eventId argument"
    }

    private val _uiState = MutableStateFlow(RoundDetailUiState())
    val uiState: StateFlow<RoundDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { getRoundEventUseCase(eventId) }
                .onSuccess { event ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        roundEvent = event,
                        isOrganizer = event.createdBy == authSessionManager.currentUserId
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "ラウンド募集詳細の取得に失敗しました"
                    )
                }
        }
    }

    fun applyJoin() {
        if (_uiState.value.isApplying) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplying = true, errorMessage = null)
            runCatching { applyRoundJoinUseCase(eventId) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isApplying = false, applySuccess = true)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isApplying = false,
                        errorMessage = error.message ?: "参加申請に失敗しました"
                    )
                }
        }
    }
}
