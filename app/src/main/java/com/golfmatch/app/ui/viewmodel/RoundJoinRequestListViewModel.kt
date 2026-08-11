package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.RoundJoinRequest
import com.golfmatch.app.domain.usecase.ApproveRoundJoinUseCase
import com.golfmatch.app.domain.usecase.GetRoundJoinRequestsUseCase
import com.golfmatch.app.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ラウンド参加申請一覧画面（主催者向け）のUiState（技術設計書7-2章 `RoundJoinRequestListUiState`）。
 *
 * [processingRequestId] は設計書のUiState定義には無いが、承認/却下処理中のボタン多重操作防止のために
 * 追加した（[RecommendUiState.sentRequestUserIds]等と同様、既存パターンの範囲内での実装判断）。
 */
data class RoundJoinRequestListUiState(
    val isLoading: Boolean = false,
    val eventId: String = "",
    val requests: List<RoundJoinRequest> = emptyList(),
    val processingRequestId: String? = null,
    val errorMessage: String? = null
)

/**
 * ラウンド参加申請一覧画面のViewModel（技術設計書6-4章、ADR-0001）。
 *
 * `created_by`本人のみ一覧取得・承認/却下が許可される（サーバー側で認可検証、6-4章）。
 */
@HiltViewModel
class RoundJoinRequestListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRoundJoinRequestsUseCase: GetRoundJoinRequestsUseCase,
    private val approveRoundJoinUseCase: ApproveRoundJoinUseCase
) : ViewModel() {

    private val eventId: String =
        checkNotNull(savedStateHandle.get<String>(Route.RoundJoinRequestList.ARG_EVENT_ID)) {
            "RoundJoinRequestListScreen requires eventId argument"
        }

    private val _uiState = MutableStateFlow(RoundJoinRequestListUiState(eventId = eventId))
    val uiState: StateFlow<RoundJoinRequestListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { getRoundJoinRequestsUseCase(eventId) }
                .onSuccess { requests ->
                    _uiState.value = _uiState.value.copy(isLoading = false, requests = requests)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "参加申請一覧の取得に失敗しました"
                    )
                }
        }
    }

    fun respond(requestId: String, approve: Boolean) {
        if (_uiState.value.processingRequestId != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(processingRequestId = requestId, errorMessage = null)
            runCatching { approveRoundJoinUseCase(eventId, requestId, approve) }
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(
                        processingRequestId = null,
                        requests = _uiState.value.requests.map { request ->
                            if (request.joinRequestId == updated.joinRequestId) updated else request
                        }
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        processingRequestId = null,
                        errorMessage = error.message ?: "参加申請の処理に失敗しました"
                    )
                }
        }
    }
}
