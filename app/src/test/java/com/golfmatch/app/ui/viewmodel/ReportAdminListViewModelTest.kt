package com.golfmatch.app.ui.viewmodel

import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.model.ReportSummary
import com.golfmatch.app.domain.repository.ReportRepository
import com.golfmatch.app.domain.usecase.GetAdminReportsUseCase
import com.golfmatch.app.testutil.FakeReportRepository
import com.golfmatch.app.testutil.MainDispatcherRule
import com.golfmatch.app.testutil.TestFixtures
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 通報管理一覧画面ViewModelのテスト（技術設計書6-9章 `GET /admin/reports`、ADR-0007）。
 *
 * サーバーのカーソル型ページネーション（`before`/`limit`）への追随を重点的に検証する。
 * `before`には末尾要素の`created_at`（ISO-8601）が渡り、サーバーはその時刻より厳密に前を返す。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportAdminListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** ViewModelのPAGE_SIZE（サーバーの`DEFAULT_PAGE_LIMIT`と同値） */
    private val pageSize = 20

    @Test
    fun `初回loadはカーソルなしで1ページ分を取得しhasMoreを立てる`() = runTest {
        val repo = PagingReportRepository(pages = listOf(summaries(0, pageSize)))

        val viewModel = ReportAdminListViewModel(GetAdminReportsUseCase(repo))

        assertEquals(1, repo.calls.size)
        assertEquals(listOf<Any?>(null, null, null, pageSize), repo.calls[0])
        assertEquals(pageSize, viewModel.uiState.value.reports.size)
        assertTrue(viewModel.uiState.value.hasMore)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadMoreは末尾のcreated_atをカーソルに次ページを末尾へ追加する`() = runTest {
        val firstPage = summaries(0, pageSize)
        val repo = PagingReportRepository(pages = listOf(firstPage, summaries(pageSize, 5)))
        val viewModel = ReportAdminListViewModel(GetAdminReportsUseCase(repo))

        viewModel.loadMore()

        assertEquals(2, repo.calls.size)
        // 時刻だけだと同時刻の通報がページ境界で取りこぼされるためIDも渡す
        assertEquals(
            listOf(null, firstPage.last().report.createdAt.toString(), firstPage.last().report.reportId, pageSize),
            repo.calls[1]
        )
        val state = viewModel.uiState.value
        assertEquals(pageSize + 5, state.reports.size)
        assertEquals("report-0", state.reports.first().report.reportId)
        assertEquals("report-${pageSize + 4}", state.reports.last().report.reportId)
        // 取得件数がPAGE_SIZE未満なので次ページは無い
        assertFalse(state.hasMore)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `1ページ目がPAGE_SIZE未満ならloadMoreしてもAPIを呼ばない`() = runTest {
        val repo = PagingReportRepository(pages = listOf(summaries(0, 3)))
        val viewModel = ReportAdminListViewModel(GetAdminReportsUseCase(repo))

        assertFalse(viewModel.uiState.value.hasMore)

        viewModel.loadMore()

        assertEquals(1, repo.calls.size)
    }

    @Test
    fun `loadMore実行中に再度呼んでもAPIは二重に呼ばれない(末尾到達の多重発火対策)`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = PagingReportRepository(
            pages = listOf(summaries(0, pageSize), summaries(pageSize, 5)),
            gateOnCursor = gate
        )
        val viewModel = ReportAdminListViewModel(GetAdminReportsUseCase(repo))

        viewModel.loadMore()
        assertTrue(viewModel.uiState.value.isLoadingMore)

        viewModel.loadMore()
        gate.complete(Unit)

        assertEquals(2, repo.calls.size)
        assertEquals(pageSize + 5, viewModel.uiState.value.reports.size)
    }

    @Test
    fun `loadMore失敗時は取得済みの一覧とhasMoreを維持したままエラーを表示する`() = runTest {
        val repo = PagingReportRepository(pages = listOf(summaries(0, pageSize)), failOnCall = 2)
        val viewModel = ReportAdminListViewModel(GetAdminReportsUseCase(repo))

        viewModel.loadMore()

        val state = viewModel.uiState.value
        assertEquals("取得失敗", state.errorMessage)
        assertEquals(pageSize, state.reports.size)
        // 末尾の再試行導線からやり直せるようhasMoreは維持する
        assertTrue(state.hasMore)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `フィルタ切り替え後に完了した古いloadMoreの結果は破棄される`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = PagingReportRepository(
            pages = listOf(summaries(0, pageSize), summaries(pageSize, 5), summaries(100, 3)),
            gateOnCursor = gate
        )
        val viewModel = ReportAdminListViewModel(GetAdminReportsUseCase(repo))

        viewModel.loadMore()
        viewModel.onStatusFilterSelected(ReportStatus.PENDING)

        assertEquals(listOf<Any?>(ReportStatus.PENDING, null, null, pageSize), repo.calls[2])
        assertEquals(3, viewModel.uiState.value.reports.size)

        gate.complete(Unit)

        val state = viewModel.uiState.value
        assertEquals(ReportStatus.PENDING, state.statusFilter)
        // 古いページ（フィルタ適用前の5件）が混入していないこと
        assertEquals(3, state.reports.size)
        assertEquals("report-100", state.reports.first().report.reportId)
        assertFalse(state.isLoadingMore)
        assertNull(state.errorMessage)
    }

    /** `created_at`降順の連番ダミーデータ（index順に古くなる） */
    private fun summaries(fromIndex: Int, count: Int): List<ReportSummary> = List(count) { offset ->
        val index = fromIndex + offset
        ReportSummary(
            report = TestFixtures.report(reportId = "report-$index")
                .copy(createdAt = Instant.fromEpochSeconds(1_800_000_000L - index)),
            reporterName = "通報者$index",
            reporterIconUrl = "",
            targetSummary = "対象$index"
        )
    }

    /**
     * ページごとの応答を返し、呼び出し引数を記録するReportRepository。
     * [gateOnCursor]を渡すとカーソル付き（＝追加読み込み）の呼び出しだけを一時停止できる。
     */
    private class PagingReportRepository(
        private val pages: List<List<ReportSummary>>,
        private val failOnCall: Int? = null,
        private val gateOnCursor: CompletableDeferred<Unit>? = null
    ) : ReportRepository by FakeReportRepository() {

        /** (statusFilter, before, before_id, limit) の呼び出し履歴 */
        val calls = mutableListOf<List<Any?>>()

        override suspend fun getAdminReports(
            statusFilter: ReportStatus?,
            before: String?,
            beforeId: String?,
            limit: Int
        ): List<ReportSummary> {
            calls += listOf(statusFilter, before, beforeId, limit)
            val callIndex = calls.size - 1
            if (before != null) gateOnCursor?.await()
            if (failOnCall == calls.size) throw RuntimeException("取得失敗")
            return pages.getOrElse(callIndex) { emptyList() }
        }
    }
}
