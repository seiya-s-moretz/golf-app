package com.golfmatch.app.data.repository.impl

import com.golfmatch.app.data.api.ApiService
import com.golfmatch.app.data.dto.SubmitReportRequestDto
import com.golfmatch.app.data.mapper.toDomain
import com.golfmatch.app.domain.model.Report
import com.golfmatch.app.domain.model.ReportReasonCategory
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
}
