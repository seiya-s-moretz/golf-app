package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.domain.usecase.SubmitReportUseCase
import com.golfmatch.app.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 通報画面のUiState（技術設計書7-2章 `ReportUiState`）。
 *
 * [reasonCategory] は設計書では`String?`だが、既存パターン（[MyPageUiState.purpose]が
 * `String`ではなく`Purpose`enumを直接保持する等）に合わせ、選択肢を型安全にするため
 * [ReportReasonCategory]enumで保持する（既存パターンの範囲内での実装判断）。
 */
data class ReportUiState(
    val targetType: ReportTargetType = ReportTargetType.USER,
    val targetId: String = "",
    val reasonCategory: ReportReasonCategory? = null,
    val reasonText: String = "",
    val isSubmitting: Boolean = false,
    val submitSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 通報画面のViewModel（技術設計書6-8章 `POST /reports`、7-3章 通報・ブロック導線）。
 *
 * 遷移元（掲示板投稿詳細・ユーザー詳細画面の「…」メニュー）から`targetType`・`targetId`を
 * Route引数として受け取る（[Route.Report]）。
 */
@HiltViewModel
class ReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val submitReportUseCase: SubmitReportUseCase
) : ViewModel() {

    private val targetType: ReportTargetType =
        checkNotNull(savedStateHandle.get<String>(Route.Report.ARG_TARGET_TYPE)) {
            "ReportScreen requires targetType argument"
        }.let { ReportTargetType.valueOf(it) }

    private val targetId: String =
        checkNotNull(savedStateHandle.get<String>(Route.Report.ARG_TARGET_ID)) {
            "ReportScreen requires targetId argument"
        }

    private val _uiState = MutableStateFlow(ReportUiState(targetType = targetType, targetId = targetId))
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun onReasonCategorySelected(category: ReportReasonCategory) {
        _uiState.value = _uiState.value.copy(reasonCategory = category, errorMessage = null)
    }

    fun onReasonTextChange(value: String) {
        _uiState.value = _uiState.value.copy(reasonText = value, errorMessage = null)
    }

    fun submit() {
        val state = _uiState.value
        // 送信中・送信成功後の二重タップを弾く（他画面と統一）。同一内容の通報が重複すると
        // 運営の通報管理一覧に同じ案件が並ぶ
        if (state.isSubmitting || state.submitSuccess) return
        val reasonCategory = state.reasonCategory
        if (reasonCategory == null) {
            _uiState.value = state.copy(errorMessage = "通報理由を選択してください")
            return
        }
        if (reasonCategory == ReportReasonCategory.OTHER && state.reasonText.isBlank()) {
            _uiState.value = state.copy(errorMessage = "「その他」を選択した場合は詳細を入力してください")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching {
                submitReportUseCase(
                    targetType = state.targetType,
                    targetId = state.targetId,
                    reasonCategory = reasonCategory,
                    reasonText = state.reasonText.ifBlank { null }
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSubmitting = false, submitSuccess = true)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = error.message ?: "通報の送信に失敗しました"
                )
            }
        }
    }
}
