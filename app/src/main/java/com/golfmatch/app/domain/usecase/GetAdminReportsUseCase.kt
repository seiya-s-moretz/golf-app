package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.model.ReportSummary
import com.golfmatch.app.domain.repository.ReportRepository
import javax.inject.Inject

/**
 * 通報一覧取得（管理者向け、技術設計書6-9章 `GET /admin/reports`、ADR-0007）。
 * `is_admin=true`のユーザーのみ利用可能（サーバー側で検証、falseは403）。
 */
class GetAdminReportsUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(
        statusFilter: ReportStatus? = null,
        before: String? = null,
        limit: Int = 50
    ): List<ReportSummary> = reportRepository.getAdminReports(statusFilter, before, limit)
}
