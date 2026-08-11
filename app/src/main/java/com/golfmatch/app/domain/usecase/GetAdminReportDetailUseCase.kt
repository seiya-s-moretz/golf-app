package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.ReportDetail
import com.golfmatch.app.domain.repository.ReportRepository
import javax.inject.Inject

/** 通報詳細取得（管理者向け、技術設計書6-9章 `GET /admin/reports/{id}`、ADR-0007） */
class GetAdminReportDetailUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(reportId: String): ReportDetail = reportRepository.getAdminReportDetail(reportId)
}
