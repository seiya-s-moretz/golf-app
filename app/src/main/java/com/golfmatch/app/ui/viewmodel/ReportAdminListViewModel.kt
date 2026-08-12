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
 *
 * [isLoading]は初回/フィルタ切り替え時の全画面ローディング、[isLoadingMore]は追加読み込み中の
 * リスト末尾ローディングを表す。[hasMore]は次ページが存在し得るかで、サーバーが[hasMore]相当の
 * フラグを返さないため「取得件数がページサイズと同じなら続きがあるとみなす」推定値。
 */
data class ReportAdminListUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
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
 *
 * サーバーは`created_at`降順・`before`カーソル（ISO-8601、指定時刻より厳密に前）でページを返すため、
 * 追加読み込みでは末尾要素の`created_at`をカーソルとして渡す。
 */
@HiltViewModel
class ReportAdminListViewModel @Inject constructor(
    private val getAdminReportsUseCase: GetAdminReportsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportAdminListUiState())
    val uiState: StateFlow<ReportAdminListUiState> = _uiState.asStateFlow()

    /**
     * [load]のたびに加算し、実行中だった[loadMore]の結果を破棄するための世代番号。
     * フィルタ切り替えと追加読み込みが競合したときに、別条件のページが末尾に混入するのを防ぐ。
     */
    private var loadGeneration = 0

    init {
        load()
    }

    fun onStatusFilterSelected(status: ReportStatus?) {
        _uiState.value = _uiState.value.copy(statusFilter = status)
        load()
    }

    fun load() {
        val statusFilter = _uiState.value.statusFilter
        val generation = ++loadGeneration
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isLoadingMore = false, errorMessage = null)
            runCatching { getAdminReportsUseCase(statusFilter, before = null, limit = PAGE_SIZE) }
                .onSuccess { reports ->
                    if (generation != loadGeneration) return@onSuccess
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        reports = reports,
                        hasMore = reports.size == PAGE_SIZE
                    )
                }
                .onFailure { error ->
                    if (generation != loadGeneration) return@onFailure
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        hasMore = false,
                        errorMessage = error.message ?: "通報一覧の取得に失敗しました"
                    )
                }
        }
    }

    /**
     * 次ページを取得して末尾に追加する（リスト末尾への到達で呼ばれる）。
     * 読み込み中・次ページ無しの場合は何もしないため、呼び出し側で多重発火を気にする必要はない。
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        val cursor = state.reports.lastOrNull()?.report?.createdAt?.toString() ?: return
        val statusFilter = state.statusFilter
        val generation = loadGeneration
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true, errorMessage = null)
            runCatching { getAdminReportsUseCase(statusFilter, before = cursor, limit = PAGE_SIZE) }
                .onSuccess { page ->
                    if (generation != loadGeneration) return@onSuccess
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        reports = _uiState.value.reports + page,
                        hasMore = page.size == PAGE_SIZE
                    )
                }
                .onFailure { error ->
                    if (generation != loadGeneration) return@onFailure
                    // hasMoreは維持し、末尾の再試行ボタンから再度[loadMore]できる状態にしておく
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        errorMessage = error.message ?: "通報一覧の取得に失敗しました"
                    )
                }
        }
    }

    private companion object {
        /** サーバー側の`DEFAULT_PAGE_LIMIT`（`functions/src/lib/pagination.ts`）に合わせる */
        const val PAGE_SIZE = 20
    }
}
