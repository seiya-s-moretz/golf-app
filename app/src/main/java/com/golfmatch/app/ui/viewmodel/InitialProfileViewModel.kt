package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.Area
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.usecase.GetAreasUseCase
import com.golfmatch.app.domain.usecase.RegisterUserUseCase
import com.golfmatch.app.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** プロフィール初期登録画面のUiState（技術設計書7-2章 `InitialProfileUiState`） */
data class InitialProfileUiState(
    val name: String = "",
    val gender: String = "",
    val age: String = "",
    val areaId: String? = null,
    val areaName: String = "",
    val areaOptions: List<Area> = emptyList(),
    val averageScore: String = "",
    val purpose: Purpose = Purpose.CASUAL,
    val introduction: String = "",
    val isLoadingAreas: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * プロフィール初期登録画面のViewModel（技術設計書3-2章・7-2章、ADR-0003）。
 *
 * 入力項目はマイページ編集画面（[MyPageViewModel]）と揃える（アイコン・名前・性別・年齢・エリア・
 * アベレージスコア・目的・自己紹介、タスク指示および技術設計書7-2章）。
 * [areaName]・[areaOptions]・[isLoadingAreas] は設計書のUiState定義には無いが、
 * [MyPageUiState]と同様にエリア選択UIのために追加した（既存パターンの範囲内での実装判断）。
 */
@HiltViewModel
class InitialProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val registerUserUseCase: RegisterUserUseCase,
    private val getAreasUseCase: GetAreasUseCase
) : ViewModel() {

    private val registrationToken: String =
        checkNotNull(savedStateHandle.get<String>(Route.InitialProfile.ARG_REGISTRATION_TOKEN)) {
            "InitialProfileScreen requires registrationToken argument"
        }

    private val _uiState = MutableStateFlow(InitialProfileUiState())
    val uiState: StateFlow<InitialProfileUiState> = _uiState.asStateFlow()

    init {
        loadAreas()
    }

    private fun loadAreas() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAreas = true)
            runCatching { getAreasUseCase() }
                .onSuccess { areas ->
                    _uiState.value = _uiState.value.copy(isLoadingAreas = false, areaOptions = areas)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingAreas = false,
                        errorMessage = error.message ?: "エリア一覧の取得に失敗しました"
                    )
                }
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onGenderChange(value: String) {
        _uiState.value = _uiState.value.copy(gender = value)
    }

    fun onAgeChange(value: String) {
        _uiState.value = _uiState.value.copy(age = value)
    }

    fun onAreaSelected(area: Area) {
        _uiState.value = _uiState.value.copy(areaId = area.areaId, areaName = area.areaName)
    }

    fun onAverageScoreChange(value: String) {
        _uiState.value = _uiState.value.copy(averageScore = value)
    }

    fun onPurposeSelected(purpose: Purpose) {
        _uiState.value = _uiState.value.copy(purpose = purpose)
    }

    fun onIntroductionChange(value: String) {
        _uiState.value = _uiState.value.copy(introduction = value)
    }

    fun submit() {
        val state = _uiState.value
        // 登録中および登録成功後の二重タップを弾く。`registrationToken`は登録成功時に消費されるため、
        // 2回目の呼び出しは必ず失敗する。成功直後は画面遷移が完了するまでボタンが操作可能な状態で
        // 残るため、`isSubmitting`だけでは塞げない
        if (state.isSubmitting || state.submitSuccess) return
        val areaId = state.areaId
        val age = state.age.toIntOrNull()
        val averageScore = state.averageScore.toIntOrNull()

        if (state.name.isBlank() || areaId == null || age == null || averageScore == null) {
            _uiState.value = state.copy(errorMessage = "入力内容を確認してください")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching {
                registerUserUseCase(
                    registrationToken = registrationToken,
                    name = state.name,
                    gender = state.gender,
                    age = age,
                    areaId = areaId,
                    averageScore = averageScore,
                    purpose = state.purpose,
                    introduction = state.introduction
                )
            }.onSuccess {
                // AuthSessionManagerへのセッション保存はRegisterUserUseCase→AuthRepositoryImpl側で完了済み
                _uiState.value = _uiState.value.copy(isSubmitting = false, submitSuccess = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = error.message ?: "プロフィールの登録に失敗しました"
                )
            }
        }
    }
}
