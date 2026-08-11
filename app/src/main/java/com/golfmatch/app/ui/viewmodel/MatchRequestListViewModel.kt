package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.MatchRequest
import com.golfmatch.app.domain.repository.MatchRequestDirection
import com.golfmatch.app.domain.usecase.GetMatchRequestsUseCase
import com.golfmatch.app.domain.usecase.RespondMatchRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 受信マッチング申請一覧画面のUiState（技術設計書7-2章 `MatchRequestListUiState`）。
 *
 * [processingRequestId] は設計書のUiState定義には無いが、承認/却下処理中のボタン多重操作防止のために
 * 追加した（[RoundJoinRequestListUiState.processingRequestId]と同様、既存パターンの範囲内での実装判断）。
 */
data class MatchRequestListUiState(
    val isLoading: Boolean = false,
    val receivedRequests: List<MatchRequest> = emptyList(),
    val processingRequestId: String? = null,
    val errorMessage: String? = null
)

/**
 * 受信マッチング申請一覧画面のViewModel（技術設計書6-5章）。
 *
 * `GET /users/me/match-requests?direction=received` で自分宛の申請のみを取得する（サーバー側で
 * ログイン中ユーザーに暗黙的に絞り込まれるため、クライアント側でuserIdを指定する必要はない）。
 * 承認の認可は `to_user_id` 本人のみ（サーバー側で検証、6-5章）。
 */
@HiltViewModel
class MatchRequestListViewModel @Inject constructor(
    private val getMatchRequestsUseCase: GetMatchRequestsUseCase,
    private val respondMatchRequestUseCase: RespondMatchRequestUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchRequestListUiState())
    val uiState: StateFlow<MatchRequestListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { getMatchRequestsUseCase(MatchRequestDirection.RECEIVED) }
                .onSuccess { requests ->
                    _uiState.value = _uiState.value.copy(isLoading = false, receivedRequests = requests)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "マッチング申請一覧の取得に失敗しました"
                    )
                }
        }
    }

    fun respond(matchRequestId: String, approve: Boolean) {
        if (_uiState.value.processingRequestId != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(processingRequestId = matchRequestId, errorMessage = null)
            runCatching { respondMatchRequestUseCase(matchRequestId, approve) }
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(
                        processingRequestId = null,
                        receivedRequests = _uiState.value.receivedRequests.map { request ->
                            if (request.matchRequestId == updated.matchRequestId) updated else request
                        }
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        processingRequestId = null,
                        errorMessage = error.message ?: "マッチング申請の処理に失敗しました"
                    )
                }
        }
    }
}
