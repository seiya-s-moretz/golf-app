package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.Conversation
import com.golfmatch.app.domain.usecase.GetConversationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * メッセージ一覧画面のUiState（技術設計書7-2章 `MessageListUiState`）。
 *
 * [isLoadingMore]は追加読み込み中、[hasMore]は次ページが存在し得るかを表す。
 */
data class MessageListUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val errorMessage: String? = null
)

/**
 * メッセージ一覧画面のViewModel（技術設計書6-7章）。
 *
 * `GET /conversations` はサーバー側でログイン中ユーザーに暗黙的に絞り込まれるため、クライアント側で
 * userIdを指定する必要はない（[GetConversationsUseCase]参照）。
 * 最終更新順・カーソル型ページネーション（`before`/`before_id`）に対応する。
 */
@HiltViewModel
class MessageListViewModel @Inject constructor(
    private val getConversationsUseCase: GetConversationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageListUiState())
    val uiState: StateFlow<MessageListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isLoadingMore = false, errorMessage = null)
            runCatching { getConversationsUseCase(before = null, beforeId = null, limit = PAGE_SIZE) }
                .onSuccess { conversations ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        conversations = conversations,
                        hasMore = conversations.size == PAGE_SIZE
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasMore = false,
                        errorMessage = error.message ?: "会話一覧の取得に失敗しました"
                    )
                }
        }
    }

    /**
     * 次ページを取得して末尾に追加する（リスト末尾への到達で呼ばれる）。
     * 読み込み中・次ページ無しの場合は何もしないため、呼び出し側で多重発火を気にする必要はない。
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        // カーソルは(updated_at, 会話ID)の組。時刻だけだと同時刻の会話が境界で取りこぼされる
        val last = state.conversations.lastOrNull() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true, errorMessage = null)
            runCatching {
                getConversationsUseCase(
                    before = last.updatedAt.toString(),
                    beforeId = last.conversationId,
                    limit = PAGE_SIZE
                )
            }
                .onSuccess { conversations ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        conversations = _uiState.value.conversations + conversations,
                        // ブロック除外はサーバー側で取得後に行われるため、件数が減っていても
                        // 次ページが存在しうる。1件も返らなかった場合のみ終端とみなす
                        hasMore = conversations.isNotEmpty()
                    )
                }
                .onFailure { error ->
                    // hasMoreは維持し、再試行できる状態にしておく
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "会話一覧の取得に失敗しました"
                    )
                }
        }
    }

    private companion object {
        /** サーバー側の`DEFAULT_PAGE_LIMIT`（`functions/src/lib/pagination.ts`）に合わせる */
        const val PAGE_SIZE = 20
    }
}
