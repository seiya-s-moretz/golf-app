package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.User
import com.golfmatch.app.domain.usecase.GetBlockedUsersUseCase
import com.golfmatch.app.domain.usecase.UnblockUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ブロック済みユーザー一覧画面のUiState（技術設計書7-2章 `BlockedUsersUiState`）。
 *
 * [processingUserId] は設計書のUiState定義には無いが、ブロック解除処理中のボタン多重操作防止のために
 * 追加した（[RoundJoinRequestListUiState.processingRequestId]等と同様、既存パターンの範囲内での実装判断）。
 */
data class BlockedUsersUiState(
    val isLoading: Boolean = false,
    val blockedUsers: List<User> = emptyList(),
    val processingUserId: String? = null,
    val errorMessage: String? = null
)

/**
 * ブロック済みユーザー一覧画面のViewModel（技術設計書6-3章 `GET /users/me/blocks`・`DELETE /users/{id}/block`）。
 */
@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val unblockUserUseCase: UnblockUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedUsersUiState())
    val uiState: StateFlow<BlockedUsersUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { getBlockedUsersUseCase() }
                .onSuccess { users ->
                    _uiState.value = _uiState.value.copy(isLoading = false, blockedUsers = users)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "ブロック済みユーザー一覧の取得に失敗しました"
                    )
                }
        }
    }

    fun unblock(userId: String) {
        if (_uiState.value.processingUserId != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(processingUserId = userId, errorMessage = null)
            runCatching { unblockUserUseCase(userId) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        processingUserId = null,
                        blockedUsers = _uiState.value.blockedUsers.filterNot { it.userId == userId }
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        processingUserId = null,
                        errorMessage = error.message ?: "ブロック解除に失敗しました"
                    )
                }
        }
    }
}
