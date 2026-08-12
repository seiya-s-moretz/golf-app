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

/**
 * メッセージスレッド画面のUiState（技術設計書7-2章 `MessageThreadUiState`）。
 *
 * [messages]は**古い順（末尾が最新）**で保持する。`GET /conversations/{partnerId}/messages`は
 * `created_at`降順（新しい順）で返すため、ViewModel側で反転してから格納する。チャットUIは
 * 下端が最新であることと、送信したメッセージを末尾に追加することを前提としているため。
 *
 * [isLoadingOlder]は過去メッセージの追加読み込み中、[hasOlder]はさらに過去が存在し得るかを表す。
 * サーバーが`has_more`相当を返さないため、[hasOlder]は「取得件数がページサイズと同じなら続きがある」推定値。
 */
data class MessageThreadUiState(
    val isLoading: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val hasOlder: Boolean = false,
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
 *
 * サーバーは`created_at`降順・`before`カーソル（ISO-8601、指定時刻より厳密に前）でページを返す。
 * 本ViewModelは取得結果を反転して古い順で保持し（[MessageThreadUiState.messages]）、
 * [loadOlder]では先頭（最も古いメッセージ）の`created_at`をカーソルとして過去を遡る。
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
            _uiState.value = _uiState.value.copy(isLoading = true, isLoadingOlder = false, errorMessage = null)
            runCatching {
                val partner = getUserUseCase(partnerId)
                val messages = getMessagesUseCase(partnerId, before = null, limit = PAGE_SIZE)
                partner to messages
            }.onSuccess { (partner, messages) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    partnerName = partner.name,
                    partnerIconUrl = partner.iconUrl,
                    // APIは新しい順で返すため、末尾が最新になるよう反転して保持する
                    messages = messages.reversed(),
                    hasOlder = messages.size == PAGE_SIZE
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasOlder = false,
                    errorMessage = error.message ?: "メッセージ履歴の取得に失敗しました"
                )
            }
        }
    }

    /**
     * 先頭（最も古いメッセージ）より前のメッセージを取得して先頭に追加する。
     * スレッド上端への到達で呼ばれるが、読み込み中・これ以上過去が無い場合は何もしない。
     */
    fun loadOlder() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingOlder || !state.hasOlder) return
        val cursor = state.messages.firstOrNull()?.createdAt?.toString() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingOlder = true, errorMessage = null)
            runCatching { getMessagesUseCase(partnerId, before = cursor, limit = PAGE_SIZE) }
                .onSuccess { older ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingOlder = false,
                        messages = older.reversed() + _uiState.value.messages,
                        hasOlder = older.size == PAGE_SIZE
                    )
                }
                .onFailure { error ->
                    // hasOlderは維持し、再試行できる状態にしておく
                    _uiState.value = _uiState.value.copy(
                        isLoadingOlder = false,
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

    private companion object {
        /** 1回の取得件数。`GetMessagesUseCase`の既定値と揃えている（サーバー側の上限は100） */
        const val PAGE_SIZE = 50
    }
}
