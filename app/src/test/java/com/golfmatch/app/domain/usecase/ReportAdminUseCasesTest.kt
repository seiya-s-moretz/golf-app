package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.model.ReportSummary
import com.golfmatch.app.testutil.FakeReportRepository
import com.golfmatch.app.testutil.TestFixtures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 通報管理（簡易管理画面、管理者向け）関連UseCaseのテスト（技術設計書6-9章、ADR-0007）。
 *
 * `is_admin=true`検証自体はサーバー側の責務であり、クライアント側UseCase/Repositoryには
 * 認可ロジックの実装がない（委譲のみ）。ここではRepositoryへの委譲の正しさのみを検証する。
 */
class ReportAdminUseCasesTest {

    @Test
    fun `GetAdminReportsUseCaseはstatusFilterをそのままRepositoryへ渡す`() = runBlocking {
        val summary = ReportSummary(
            report = TestFixtures.report(status = ReportStatus.PENDING),
            reporterName = "山田太郎",
            reporterIconUrl = "https://example.com/icon.png",
            targetSummary = "鈴木花子"
        )
        val repo = FakeReportRepository(adminReports = listOf(summary))
        val useCase = GetAdminReportsUseCase(repo)

        val result = useCase(ReportStatus.PENDING)

        assertEquals(ReportStatus.PENDING, repo.lastAdminReportsStatusFilter)
        assertEquals(listOf(summary), result)
    }

    @Test
    fun `GetAdminReportsUseCaseはstatusFilter未指定(null)の場合もnullのままRepositoryへ渡す(全件取得)`() = runBlocking {
        val repo = FakeReportRepository()
        val useCase = GetAdminReportsUseCase(repo)

        useCase()

        assertNull(repo.lastAdminReportsStatusFilter)
    }

    @Test
    fun `GetAdminReportDetailUseCaseはreportIdをそのままRepositoryへ渡す`() = runBlocking {
        val detail = TestFixtures.reportDetail()
        val repo = FakeReportRepository(adminReportDetail = detail)
        val useCase = GetAdminReportDetailUseCase(repo)

        val result = useCase("report-9")

        assertEquals("report-9", repo.lastAdminReportDetailId)
        assertEquals(detail, result)
    }

    @Test
    fun `UpdateReportStatusUseCaseは全パラメータをそのままRepositoryへ渡す(handlingMemo非null)`() = runBlocking {
        val repo = FakeReportRepository()
        val useCase = UpdateReportStatusUseCase(repo)

        useCase("report-1", ReportStatus.RESOLVED, "本人へ警告済み")

        assertEquals(Triple("report-1", ReportStatus.RESOLVED, "本人へ警告済み"), repo.lastUpdateStatusArgs)
    }

    @Test
    fun `UpdateReportStatusUseCaseはhandlingMemoがnullの場合もnullのままRepositoryへ渡す`() = runBlocking {
        val repo = FakeReportRepository()
        val useCase = UpdateReportStatusUseCase(repo)

        useCase("report-1", ReportStatus.DISMISSED, null)

        assertEquals(Triple("report-1", ReportStatus.DISMISSED, null), repo.lastUpdateStatusArgs)
    }
}
