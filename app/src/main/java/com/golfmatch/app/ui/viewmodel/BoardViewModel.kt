package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.BoardPost
import com.golfmatch.app.domain.model.User
import com.golfmatch.app.domain.usecase.BlockUserUseCase
import com.golfmatch.app.domain.usecase.GetBoardPostsUseCase
import com.golfmatch.app.domain.usecase.GetUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 掲示板画面のUiState（`D:\勉強\golf\UiState定義.md` 4章、技術設計書7章）。
 *
 * [authors] は設計書のUiState定義には無いが、[BoardPost]が投稿者名・アイコンを含まず`userId`のみ
 * 保持するため、投稿者表示のために追加した（実装メモ参照。既存パターンの範囲内での実装判断）。
 *
 * [isLoadingMore]は追加読み込み中、[hasMore]は次ページが存在し得るかを表す。サーバーが`has_more`相当を
 * 返さないため、[hasMore]は「取得件数がページサイズと同じなら続きがある」推定値。
 */
data class BoardUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val posts: List<BoardPost> = emptyList(),
    val authors: Map<String, User> = emptyMap(),
    /** ブロック処理中のユーザーID（連打による多重実行を防ぐ） */
    val blockingUserIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

/**
 * 掲示板画面のViewModel（技術設計書6-6章）。
 *
 * サーバーは`created_at`降順・`before`カーソル（指定時刻より厳密に前）でページを返すため、
 * 追加読み込みでは末尾要素の`created_at`をカーソルとして渡す。
 */
@HiltViewModel
class BoardViewModel @Inject constructor(
    private val getBoardPostsUseCase: GetBoardPostsUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val blockUserUseCase: BlockUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardUiState())
    val uiState: StateFlow<BoardUiState> = _uiState.asStateFlow()

    init {
        loadBoardPosts()
    }

    fun loadBoardPosts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isLoadingMore = false, errorMessage = null)
            runCatching {
                val posts = getBoardPostsUseCase(before = null, beforeId = null, limit = PAGE_SIZE)
                posts to fetchAuthors(posts, emptyMap())
            }.onSuccess { (posts, authors) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    posts = posts,
                    authors = authors,
                    hasMore = posts.size == PAGE_SIZE
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasMore = false,
                    errorMessage = error.message ?: "掲示板投稿一覧の取得に失敗しました"
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
        // カーソルは(created_at, ID)の組。時刻だけだと同時刻の投稿がページ境界で取りこぼされる
        val last = state.posts.lastOrNull() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true, errorMessage = null)
            runCatching {
                val posts = getBoardPostsUseCase(
                    before = last.createdAt.toString(),
                    beforeId = last.postId,
                    limit = PAGE_SIZE
                )
                posts to fetchAuthors(posts, _uiState.value.authors)
            }.onSuccess { (posts, authors) ->
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    posts = _uiState.value.posts + posts,
                    authors = authors,
                    // ブロック除外はサーバー側で取得後に行われるため、件数が減っていても
                    // 次ページが存在しうる。1件も返らなかった場合のみ終端とみなす
                    hasMore = posts.isNotEmpty()
                )
            }.onFailure { error ->
                // hasMoreは維持し、再試行できる状態にしておく
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    errorMessage = error.message ?: "掲示板投稿一覧の取得に失敗しました"
                )
            }
        }
    }

    /**
     * 投稿者をブロックする（技術設計書7-3章の通報・ブロック導線、5-2章）。
     * サーバー側の`GET /board`はブロック関係のユーザーの投稿を除外するため、
     * 成功後は手元の一覧からも即時に取り除く（再取得を待たずに反映する）。
     */
    fun blockUser(userId: String) {
        if (userId in _uiState.value.blockingUserIds) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                blockingUserIds = _uiState.value.blockingUserIds + userId,
                errorMessage = null
            )
            runCatching { blockUserUseCase(userId) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        posts = _uiState.value.posts.filterNot { it.userId == userId },
                        blockingUserIds = _uiState.value.blockingUserIds - userId
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        blockingUserIds = _uiState.value.blockingUserIds - userId,
                        errorMessage = error.message ?: "ブロックに失敗しました"
                    )
                }
        }
    }

    /** 未取得の投稿者だけを追加で取得する（取得済みの分は使い回す） */
    private suspend fun fetchAuthors(posts: List<BoardPost>, known: Map<String, User>): Map<String, User> {
        val missingIds = posts.map { it.userId }.distinct().filterNot { known.containsKey(it) }
        val fetched = missingIds
            .mapNotNull { userId -> runCatching { getUserUseCase(userId) }.getOrNull() }
            .associateBy { it.userId }
        return known + fetched
    }

    private companion object {
        /** サーバー側の`DEFAULT_PAGE_LIMIT`（`functions/src/lib/pagination.ts`）に合わせる */
        const val PAGE_SIZE = 20
    }
}
