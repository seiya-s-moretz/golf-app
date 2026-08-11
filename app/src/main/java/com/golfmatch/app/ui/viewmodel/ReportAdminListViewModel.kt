package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.model.ReportSummary
import com.golfmatch.app.domain.usecase.GetAdminReportsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 通報管理一覧画面（管理者向け）のUiState（技術設計書7-2章 `ReportAdminListUiState`）。
 *
 * [statusFilter]がnullの場合は全件表示。`null`/`PENDING`/`REVIEWING`/`RESOLVED`/`DISMISSED`の
 * 簡易タブ切り替えで絞り込む（技術設計書7-4章）。
 */
data class ReportAdminListUiState(
    val isLoading: Boolean = false,
    val statusFilter: ReportStatus? = null,
    val reports: List<ReportSummary> = emptyList(),
    val errorMessage: String? = null
)

/**
 * 通報管理一覧画面のViewModel（技術設計書6-9章 `GET /admin/reports`、ADR-0007）。
 *
 * `User.is_admin=true`のユーザーのみマイページからこの画面に到達できる（技術設計書3-4章）。
 * `is_admin=false`でこの画面のAPIが呼ばれた場合はサーバー側で403となるが、特別なUIハンドリングは設けず
 * 既存のエラーハンドリングパターン（[errorMessage]表示）に委ねる。
 */
@HiltViewModel
class ReportAdminListViewModel @Inject constructor(
    private val getAdminReportsUseCase: GetAdminReportsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportAdminListUiState())
    val uiState: StateFlow<ReportAdminListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onStatusFilterSelected(status: ReportStatus?) {
        _uiState.value = _uiState.value.copy(statusFilter = status)
        load()
    }

    fun load() {
        val statusFilter = _uiState.value.statusFilter
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { getAdminReportsUseCase(statusFilter) }
                .onSuccess { reports ->
                    _uiState.value = _uiState.value.copy(isLoading = false, reports = reports)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "通報一覧の取得に失敗しました"
                    )
                }
        }
    }
}
