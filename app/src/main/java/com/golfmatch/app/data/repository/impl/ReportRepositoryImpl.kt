package com.golfmatch.app.data.repository.impl

import com.golfmatch.app.data.api.ApiService
import com.golfmatch.app.data.dto.SubmitReportRequestDto
import com.golfmatch.app.data.dto.UpdateReportStatusRequestDto
import com.golfmatch.app.data.mapper.toDomain
import com.golfmatch.app.domain.model.Report
import com.golfmatch.app.domain.model.ReportDetail
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.model.ReportSummary
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.domain.repository.ReportRepository
import javax.inject.Inject

class ReportRepositoryImpl @Inject constructor(
    private val api: ApiService
) : ReportRepository {

    override suspend fun submitReport(
        targetType: ReportTargetType,
        targetId: String,
        reasonCategory: ReportReasonCategory,
        reasonText: String?
    ): Report = api.submitReport(
        SubmitReportRequestDto(
            targetType = targetType.name,
            targetId = targetId,
            reasonCategory = reasonCategory.name,
            reasonText = reasonText
        )
    ).toDomain()

    override suspend fun getAdminReports(statusFilter: ReportStatus?, before: String?, limit: Int): List<ReportSummary> =
        api.getAdminReports(statusFilter?.name, before, limit).map { it.toDomain() }

    override suspend fun getAdminReportDetail(reportId: String): ReportDetail =
        api.getAdminReportDetail(reportId).toDomain()

    override suspend fun updateReportStatus(
        reportId: String,
        status: ReportStatus,
        handlingMemo: String?
    ): ReportDetail = api.updateReportStatus(
        reportId,
        UpdateReportStatusRequestDto(status = status.name, handlingMemo = handlingMemo)
    ).toDomain()
}
