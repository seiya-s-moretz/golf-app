package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.User
import com.golfmatch.app.domain.usecase.BlockUserUseCase
import com.golfmatch.app.domain.usecase.GetAreasUseCase
import com.golfmatch.app.domain.usecase.GetRecommendUsersUseCase
import com.golfmatch.app.domain.usecase.SendMatchRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * おすすめユーザー画面のUiState（`D:\勉強\golf\UiState定義.md` 3章、技術設計書7-1章）。
 *
 * [areaNames] と [sentRequestUserIds] は設計書のUiState定義には無いが、カード表示（住居エリア名）と
 * マッチング申請後の状態変化表示（DeveloperAgentタスク要件）のために追加した（既存パターンの範囲内での実装判断）。
 */
data class RecommendUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val users: List<User> = emptyList(),
    val areaNames: Map<String, String> = emptyMap(),
    val sentRequestUserIds: Set<String> = emptySet(),
    /** マッチング申請の送信中（レスポンス待ち）のユーザーID。連打による二重送信を防ぐ */
    val sendingRequestUserIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

@HiltViewModel
class RecommendViewModel @Inject constructor(
    private val getRecommendUsersUseCase: GetRecommendUsersUseCase,
    private val getAreasUseCase: GetAreasUseCase,
    private val sendMatchRequestUseCase: SendMatchRequestUseCase,
    private val blockUserUseCase: BlockUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendUiState())
    val uiState: StateFlow<RecommendUiState> = _uiState.asStateFlow()

    init {
        loadRecommendUsers()
    }

    fun loadRecommendUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isLoadingMore = false, errorMessage = null)
            runCatching {
                val users = getRecommendUsersUseCase(beforeId = null, limit = PAGE_SIZE)
                val areaNames = getAreasUseCase().associate { it.areaId to it.areaName }
                users to areaNames
            }.onSuccess { (users, areaNames) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    users = users,
                    areaNames = areaNames,
                    hasMore = users.size == PAGE_SIZE
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasMore = false,
                    errorMessage = error.message ?: "おすすめユーザー一覧の取得に失敗しました"
                )
            }
        }
    }

    /**
     * 次ページを取得して末尾に追加する（リスト末尾への到達で呼ばれる）。
     *
     * おすすめのカーソルは時刻ではなく**前ページ最後のユーザーID**（スコアはサーバー計算値で
     * 保存されていないため）。目印のユーザーがブロック等で消えるとサーバーは先頭から返すので、
     * **`userId`で重複排除してから追加する**。
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        val lastUserId = state.users.lastOrNull()?.userId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true, errorMessage = null)
            runCatching { getRecommendUsersUseCase(beforeId = lastUserId, limit = PAGE_SIZE) }
                .onSuccess { users ->
                    val current = _uiState.value.users
                    val knownIds = current.map { it.userId }.toSet()
                    val added = users.filterNot { it.userId in knownIds }
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        users = current + added,
                        // 重複排除で0件になっても「取得結果が空でない限り続きがある」とみなすと
                        // 無限ループになるため、実際に追加できた件数で判定する
                        hasMore = added.isNotEmpty()
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "おすすめユーザー一覧の取得に失敗しました"
                    )
                }
        }
    }

    fun sendMatchRequest(user: User) {
        val state = _uiState.value
        // 送信完了を待たずに再タップすると2回送信され、サーバーの重複チェック（409）により
        // 「既に申請済みです」というエラーが表示される。申請自体は成功しているのに失敗したように
        // 見えるため、送信中のユーザーIDを保持して弾く
        if (user.userId in state.sentRequestUserIds || user.userId in state.sendingRequestUserIds) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                sendingRequestUserIds = _uiState.value.sendingRequestUserIds + user.userId,
                errorMessage = null
            )
            runCatching { sendMatchRequestUseCase(user.userId) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        sentRequestUserIds = _uiState.value.sentRequestUserIds + user.userId,
                        sendingRequestUserIds = _uiState.value.sendingRequestUserIds - user.userId
                    )
                }
                .onFailure { error ->
                    // 失敗時は送信中フラグを戻し、再試行できるようにする
                    _uiState.value = _uiState.value.copy(
                        sendingRequestUserIds = _uiState.value.sendingRequestUserIds - user.userId,
                        errorMessage = error.message ?: "マッチング申請の送信に失敗しました"
                    )
                }
        }
    }

    /**
     * ユーザーブロック（技術設計書7-3章、[UserCard][com.golfmatch.app.ui.component.UserCard]の
     * 確認ダイアログから呼び出される）。ブロック成功後は一覧から即時除外する（サーバー側の
     * `GET /users/recommend`除外仕様、技術設計書6-5章を先取りしたUX上の即時反映）。
     */
    fun blockUser(user: User) {
        viewModelScope.launch {
            runCatching { blockUserUseCase(user.userId) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        users = _uiState.value.users.filterNot { it.userId == user.userId }
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "ブロックに失敗しました"
                    )
                }
        }
    }

    private companion object {
        /** サーバー側の`DEFAULT_PAGE_LIMIT`（`functions/src/lib/pagination.ts`）に合わせる */
        const val PAGE_SIZE = 20
    }
}
