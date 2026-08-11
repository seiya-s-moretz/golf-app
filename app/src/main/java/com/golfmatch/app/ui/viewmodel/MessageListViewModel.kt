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

/** メッセージ一覧画面のUiState（技術設計書7-2章 `MessageListUiState`） */
data class MessageListUiState(
    val isLoading: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val errorMessage: String? = null
)

/**
 * メッセージ一覧画面のViewModel（技術設計書6-7章）。
 *
 * `GET /conversations` はサーバー側でログイン中ユーザーに暗黙的に絞り込まれるため、クライアント側で
 * userIdを指定する必要はない（[GetConversationsUseCase]参照）。
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
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { getConversationsUseCase() }
                .onSuccess { conversations ->
                    _uiState.value = _uiState.value.copy(isLoading = false, conversations = conversations)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "会話一覧の取得に失敗しました"
                    )
                }
        }
    }
}
