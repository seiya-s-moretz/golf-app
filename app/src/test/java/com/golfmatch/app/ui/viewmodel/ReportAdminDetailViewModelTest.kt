package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.golfmatch.app.domain.model.ReportDetail
import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.repository.ReportRepository
import com.golfmatch.app.domain.usecase.GetAdminReportDetailUseCase
import com.golfmatch.app.domain.usecase.UpdateReportStatusUseCase
import com.golfmatch.app.testutil.FakeReportRepository
import com.golfmatch.app.testutil.MainDispatcherRule
import com.golfmatch.app.testutil.TestFixtures
import com.golfmatch.app.ui.navigation.Route
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 通報管理詳細画面（管理者向け）ViewModelのテスト（技術設計書6-9章、ADR-0007）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportAdminDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun savedStateHandle(reportId: String = "report-1") =
        SavedStateHandle(mapOf(Route.ReportAdminDetail.ARG_REPORT_ID to reportId))

    @Test
    fun `loadで取得した通報詳細のstatus_memoがselectedStatus_handlingMemoの初期値になる`() = runTest {
        val detail = TestFixtures.reportDetail(
            report = TestFixtures.report(status = ReportStatus.REVIEWING).copy(handlingMemo = "確認中です")
        )
        val repo = FakeReportRepository(adminReportDetail = detail)
        val viewModel = ReportAdminDetailViewModel(
            savedStateHandle(),
            GetAdminReportDetailUseCase(repo),
            UpdateReportStatusUseCase(repo)
        )

        assertEquals(ReportStatus.REVIEWING, viewModel.uiState.value.selectedStatus)
        assertEquals("確認中です", viewModel.uiState.value.handlingMemo)
        assertEquals(detail, viewModel.uiState.value.report)
    }

    @Test
    fun `handlingMemoが未設定(null)の場合は空文字が初期値になる`() = runTest {
        val detail = TestFixtures.reportDetail(report = TestFixtures.report().copy(handlingMemo = null))
        val repo = FakeReportRepository(adminReportDetail = detail)
        val viewModel = ReportAdminDetailViewModel(
            savedStateHandle(),
            GetAdminReportDetailUseCase(repo),
            UpdateReportStatusUseCase(repo)
        )

        assertEquals("", viewModel.uiState.value.handlingMemo)
    }

    @Test
    fun `saveはselectedStatusとhandlingMemoでUseCaseを呼びレスポンスでuiStateを更新する`() = runTest {
        val initialDetail = TestFixtures.reportDetail()
        val updatedDetail = TestFixtures.reportDetail(
            report = TestFixtures.report(status = ReportStatus.RESOLVED).copy(handlingMemo = "対応完了")
        )
        val repo = FakeReportRepository(adminReportDetail = initialDetail)
        val viewModel = ReportAdminDetailViewModel(
            savedStateHandle(),
            GetAdminReportDetailUseCase(repo),
            UpdateReportStatusUseCase(repo)
        )

        // FakeReportRepositoryのupdateReportStatusは固定のadminReportDetailしか返せないため、
        // ここではUseCaseへ渡す引数の正しさを検証する
        viewModel.onStatusSelected(ReportStatus.RESOLVED)
        viewModel.onHandlingMemoChange("対応完了")
        viewModel.save()

        assertEquals(Triple("report-1", ReportStatus.RESOLVED, "対応完了"), repo.lastUpdateStatusArgs)
        assertTrue(viewModel.uiState.value.updateSuccess)
    }

    @Test
    fun `handlingMemoが空文字の場合はnullとしてUseCaseへ渡される`() = runTest {
        val repo = FakeReportRepository()
        val viewModel = ReportAdminDetailViewModel(
            savedStateHandle(),
            GetAdminReportDetailUseCase(repo),
            UpdateReportStatusUseCase(repo)
        )

        viewModel.onHandlingMemoChange("   ")
        viewModel.save()

        assertEquals(null, repo.lastUpdateStatusArgs?.third)
    }

    /**
     * 【修正済み・4-4章対応】ReportAdminDetailViewModel.save()は、[RoundJoinRequestListViewModel.respond]や
     * [MatchRequestListViewModel.respond]、[BlockedUsersViewModel.unblock]と同様の
     * `isUpdating`による多重操作防止ガード（`save()`冒頭の早期return）を持つ。
     * このテストは、保存処理中に再度`save()`を呼んでもUseCaseが1回しか呼ばれないこと
     * （＝ガードが機能していること）を確認する回帰検知用テスト。
     */
    @Test
    fun `save処理中に再度saveを呼んでもUseCaseは1回しか呼ばれない(多重操作防止ガード)`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var updateCallCount = 0
        val repo = object : ReportRepository by FakeReportRepository() {
            override suspend fun getAdminReportDetail(reportId: String): ReportDetail = TestFixtures.reportDetail()

            override suspend fun updateReportStatus(reportId: String, status: ReportStatus, handlingMemo: String?): ReportDetail {
                updateCallCount++
                gate.await()
                return TestFixtures.reportDetail(report = TestFixtures.report(status = status))
            }
        }
        val viewModel = ReportAdminDetailViewModel(
            savedStateHandle(),
            GetAdminReportDetailUseCase(repo),
            UpdateReportStatusUseCase(repo)
        )

        viewModel.save()
        assertTrue(viewModel.uiState.value.isUpdating)

        // 1回目が完了する前に2回目を呼んでも、ガードにより無視される
        viewModel.save()

        gate.complete(Unit)

        assertEquals(1, updateCallCount)
    }
}
