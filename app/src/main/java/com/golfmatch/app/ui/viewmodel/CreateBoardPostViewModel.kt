package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.usecase.PostBoardMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 掲示板新規投稿画面のUiState（`D:\勉強\golf\UiState定義.md` 5章、技術設計書7章） */
data class CreateBoardPostUiState(
    val content: String = "",
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class CreateBoardPostViewModel @Inject constructor(
    private val postBoardMessageUseCase: PostBoardMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateBoardPostUiState())
    val uiState: StateFlow<CreateBoardPostUiState> = _uiState.asStateFlow()

    fun onContentChange(content: String) {
        _uiState.value = _uiState.value.copy(content = content, errorMessage = null)
    }

    fun submit() {
        // 送信中・送信成功後の二重タップを弾く（他画面と統一）。同じ内容の投稿が2件並ぶのを防ぐ
        if (_uiState.value.isSubmitting || _uiState.value.submitSuccess) return
        val content = _uiState.value.content.trim()
        if (content.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "投稿内容を入力してください")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching { postBoardMessageUseCase(content) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, submitSuccess = true)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "投稿に失敗しました"
                    )
                }
        }
    }
}
