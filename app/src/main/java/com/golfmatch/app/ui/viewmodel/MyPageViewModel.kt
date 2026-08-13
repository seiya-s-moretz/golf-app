package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.data.auth.AuthSessionManager
import com.golfmatch.app.domain.model.Area
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.usecase.GetAreasUseCase
import com.golfmatch.app.domain.usecase.GetUserUseCase
import com.golfmatch.app.domain.usecase.UpdateUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * マイページ画面のUiState（技術設計書7-1章 `MyPageUiState`）。
 *
 * [areaOptions] と [isLoading] は設計書のUiState定義には無いが、エリア選択編集UI（[areaOptions]）と
 * 初期プロフィール取得中の状態表現（[isLoading]）のために追加した（実装メモ参照。既存パターンの範囲内での実装判断）。
 */
data class MyPageUiState(
    val isLoading: Boolean = false,
    val iconUrl: String = "",
    val name: String = "",
    val gender: String = "",
    val age: String = "",
    val areaId: String? = null,
    val areaName: String = "",
    val areaOptions: List<Area> = emptyList(),
    val averageScore: String = "",
    val isAverageScoreSelfReported: Boolean = true,
    val purpose: Purpose = Purpose.CASUAL,
    val introduction: String = "",
    val phoneVerified: Boolean = false,
    val isAdmin: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val getAreasUseCase: GetAreasUseCase,
    private val authSessionManager: AuthSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val userId = authSessionManager.currentUserId
        if (userId == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "ログイン情報が見つかりません。再度ログインしてください"
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val user = getUserUseCase(userId)
                val areas = getAreasUseCase()
                user to areas
            }.onSuccess { (user, areas) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    iconUrl = user.iconUrl,
                    name = user.name,
                    gender = user.gender,
                    age = user.age.toString(),
                    areaId = user.areaId,
                    areaName = areas.firstOrNull { it.areaId == user.areaId }?.areaName.orEmpty(),
                    areaOptions = areas,
                    averageScore = user.averageScore.toString(),
                    purpose = user.purpose,
                    introduction = user.introduction,
                    phoneVerified = user.phoneVerified,
                    isAdmin = user.isAdmin
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "プロフィールの取得に失敗しました"
                )
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, saveSuccess = false)
    }

    fun onGenderChange(value: String) {
        _uiState.value = _uiState.value.copy(gender = value, saveSuccess = false)
    }

    fun onAgeChange(value: String) {
        _uiState.value = _uiState.value.copy(age = value, saveSuccess = false)
    }

    fun onAreaSelected(area: Area) {
        _uiState.value = _uiState.value.copy(areaId = area.areaId, areaName = area.areaName, saveSuccess = false)
    }

    fun onAverageScoreChange(value: String) {
        _uiState.value = _uiState.value.copy(averageScore = value, saveSuccess = false)
    }

    fun onPurposeSelected(purpose: Purpose) {
        _uiState.value = _uiState.value.copy(purpose = purpose, saveSuccess = false)
    }

    fun onIntroductionChange(value: String) {
        _uiState.value = _uiState.value.copy(introduction = value, saveSuccess = false)
    }

    fun save() {
        val userId = authSessionManager.currentUserId
        val state = _uiState.value
        // 保存中の二重タップを弾く（他画面と統一）。プロフィール更新は冪等なので実害は小さいが、
        // 無駄なリクエストと「どちらの結果が最終表示か不定」な状態を避ける。
        // `saveSuccess`は編集操作でfalseに戻るため、保存後の再保存は妨げない
        if (state.isSaving) return
        val areaId = state.areaId
        val age = state.age.toIntOrNull()
        val averageScore = state.averageScore.toIntOrNull()

        if (userId == null) {
            _uiState.value = state.copy(errorMessage = "ログイン情報が見つかりません。再度ログインしてください")
            return
        }
        if (state.name.isBlank() || areaId == null || age == null || averageScore == null) {
            _uiState.value = state.copy(errorMessage = "入力内容を確認してください")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, saveSuccess = false)
            runCatching {
                updateUserProfileUseCase(
                    userId = userId,
                    name = state.name,
                    gender = state.gender,
                    age = age,
                    areaId = areaId,
                    averageScore = averageScore,
                    purpose = state.purpose,
                    introduction = state.introduction
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = error.message ?: "プロフィールの保存に失敗しました"
                )
            }
        }
    }
}
