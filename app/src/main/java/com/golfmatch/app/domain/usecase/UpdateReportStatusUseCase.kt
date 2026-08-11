package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.ReportDetail
import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.repository.ReportRepository
import javax.inject.Inject

/**
 * 通報ステータスの変更（管理者向け、技術設計書6-9章 `PATCH /admin/reports/{id}/status`、ADR-0007）。
 * MVPでは状態遷移順序の強制は行わない（過剰設計を避ける方針）。
 */
class UpdateReportStatusUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(reportId: String, status: ReportStatus, handlingMemo: String?): ReportDetail =
        reportRepository.updateReportStatus(reportId, status, handlingMemo)
}
