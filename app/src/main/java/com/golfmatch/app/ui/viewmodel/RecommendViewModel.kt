package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.User
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
    val users: List<User> = emptyList(),
    val areaNames: Map<String, String> = emptyMap(),
    val sentRequestUserIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

@HiltViewModel
class RecommendViewModel @Inject constructor(
    private val getRecommendUsersUseCase: GetRecommendUsersUseCase,
    private val getAreasUseCase: GetAreasUseCase,
    private val sendMatchRequestUseCase: SendMatchRequestUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendUiState())
    val uiState: StateFlow<RecommendUiState> = _uiState.asStateFlow()

    init {
        loadRecommendUsers()
    }

    fun loadRecommendUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val users = getRecommendUsersUseCase()
                val areaNames = getAreasUseCase().associate { it.areaId to it.areaName }
                users to areaNames
            }.onSuccess { (users, areaNames) ->
                _uiState.value = _uiState.value.copy(isLoading = false, users = users, areaNames = areaNames)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "おすすめユーザー一覧の取得に失敗しました"
                )
            }
        }
    }

    fun sendMatchRequest(user: User) {
        if (user.userId in _uiState.value.sentRequestUserIds) return
        viewModelScope.launch {
            runCatching { sendMatchRequestUseCase(user.userId) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        sentRequestUserIds = _uiState.value.sentRequestUserIds + user.userId
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "マッチング申請の送信に失敗しました"
                    )
                }
        }
    }
}
