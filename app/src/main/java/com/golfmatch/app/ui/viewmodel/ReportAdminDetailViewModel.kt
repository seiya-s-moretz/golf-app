package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.ReportDetail
import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.usecase.GetAdminReportDetailUseCase
import com.golfmatch.app.domain.usecase.UpdateReportStatusUseCase
import com.golfmatch.app.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 通報管理詳細画面（管理者向け）のUiState（技術設計書7-2章 `ReportAdminDetailUiState`）。
 *
 * [selectedStatus]・[handlingMemo]は更新操作中の編集値（技術設計書7-4章）。
 */
data class ReportAdminDetailUiState(
    val isLoading: Boolean = false,
    val reportId: String = "",
    val report: ReportDetail? = null,
    val selectedStatus: ReportStatus = ReportStatus.PENDING,
    val handlingMemo: String = "",
    val isUpdating: Boolean = false,
    val updateSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 通報管理詳細画面のViewModel（技術設計書6-9章 `GET /admin/reports/{id}` `PATCH /admin/reports/{id}/status`、ADR-0007）。
 *
 * MVPでは状態遷移順序の強制を行わない（任意のステータス値へ変更可能。過剰設計を避ける方針、技術設計書6-9章）。
 */
@HiltViewModel
class ReportAdminDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAdminReportDetailUseCase: GetAdminReportDetailUseCase,
    private val updateReportStatusUseCase: UpdateReportStatusUseCase
) : ViewModel() {

    private val reportId: String =
        checkNotNull(savedStateHandle.get<String>(Route.ReportAdminDetail.ARG_REPORT_ID)) {
            "ReportAdminDetailScreen requires reportId argument"
        }

    private val _uiState = MutableStateFlow(ReportAdminDetailUiState(reportId = reportId))
    val uiState: StateFlow<ReportAdminDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { getAdminReportDetailUseCase(reportId) }
                .onSuccess { detail ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        report = detail,
                        selectedStatus = detail.report.status,
                        handlingMemo = detail.report.handlingMemo.orEmpty()
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "通報詳細の取得に失敗しました"
                    )
                }
        }
    }

    fun onStatusSelected(status: ReportStatus) {
        _uiState.value = _uiState.value.copy(selectedStatus = status, updateSuccess = false)
    }

    fun onHandlingMemoChange(value: String) {
        _uiState.value = _uiState.value.copy(handlingMemo = value, updateSuccess = false)
    }

    fun save() {
        val state = _uiState.value
        if (state.isUpdating) return
        viewModelScope.launch {
            _uiState.value = state.copy(isUpdating = true, errorMessage = null, updateSuccess = false)
            runCatching {
                updateReportStatusUseCase(reportId, state.selectedStatus, state.handlingMemo.ifBlank { null })
            }.onSuccess { detail ->
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    updateSuccess = true,
                    report = detail,
                    selectedStatus = detail.report.status,
                    handlingMemo = detail.report.handlingMemo.orEmpty()
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    errorMessage = error.message ?: "通報ステータスの更新に失敗しました"
                )
            }
        }
    }
}
