package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.BoardPost
import com.golfmatch.app.domain.model.User
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
 */
data class BoardUiState(
    val isLoading: Boolean = false,
    val posts: List<BoardPost> = emptyList(),
    val authors: Map<String, User> = emptyMap(),
    val errorMessage: String? = null
)

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val getBoardPostsUseCase: GetBoardPostsUseCase,
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoardUiState())
    val uiState: StateFlow<BoardUiState> = _uiState.asStateFlow()

    init {
        loadBoardPosts()
    }

    fun loadBoardPosts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val posts = getBoardPostsUseCase()
                val authors = posts.map { it.userId }.distinct()
                    .mapNotNull { userId -> runCatching { getUserUseCase(userId) }.getOrNull() }
                    .associateBy { it.userId }
                posts to authors
            }.onSuccess { (posts, authors) ->
                _uiState.value = _uiState.value.copy(isLoading = false, posts = posts, authors = authors)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "掲示板投稿一覧の取得に失敗しました"
                )
            }
        }
    }
}
