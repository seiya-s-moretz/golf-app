package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.Message
import com.golfmatch.app.domain.usecase.GetMessagesUseCase
import com.golfmatch.app.domain.usecase.GetUserUseCase
import com.golfmatch.app.domain.usecase.SendMessageUseCase
import com.golfmatch.app.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** メッセージスレッド画面のUiState（技術設計書7-2章 `MessageThreadUiState`） */
data class MessageThreadUiState(
    val isLoading: Boolean = false,
    val partnerId: String = "",
    val partnerName: String = "",
    val partnerIconUrl: String = "",
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

/**
 * メッセージスレッド画面のViewModel（技術設計書6-7章）。
 *
 * `GET /conversations/{partnerId}/messages` は相手ユーザー情報を含まないため、相手の表示名・アイコンは
 * [GetUserUseCase]で別途取得する（[MyPageViewModel]等と同様、既存パターンの範囲内での実装判断）。
 * 対象ユーザーペア間に`Connection`が存在しない場合はサーバー側で403となり、[uiState.errorMessage]に反映する。
 * 既読化（`POST /conversations/{partnerId}/read`）は技術設計書のUiStateに既読状態フィールドが無いため、
 * 本実装のスコープに含めない（表示のみ）。
 */
@HiltViewModel
class MessageThreadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val getUserUseCase: GetUserUseCase
) : ViewModel() {

    private val partnerId: String =
        checkNotNull(savedStateHandle.get<String>(Route.MessageThread.ARG_PARTNER_ID)) {
            "MessageThreadScreen requires partnerId argument"
        }

    private val _uiState = MutableStateFlow(MessageThreadUiState(partnerId = partnerId))
    val uiState: StateFlow<MessageThreadUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val partner = getUserUseCase(partnerId)
                val messages = getMessagesUseCase(partnerId)
                partner to messages
            }.onSuccess { (partner, messages) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    partnerName = partner.name,
                    partnerIconUrl = partner.iconUrl,
                    messages = messages
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "メッセージ履歴の取得に失敗しました"
                )
            }
        }
    }

    fun onInputTextChange(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun send() {
        val content = _uiState.value.inputText.trim()
        if (content.isEmpty() || _uiState.value.isSending) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, errorMessage = null)
            runCatching { sendMessageUseCase(partnerId, content) }
                .onSuccess { sent ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        inputText = "",
                        messages = _uiState.value.messages + sent
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        errorMessage = error.message ?: "メッセージの送信に失敗しました"
                    )
                }
        }
    }
}
