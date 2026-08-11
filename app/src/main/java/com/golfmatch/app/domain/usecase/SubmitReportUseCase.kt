package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.Report
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.domain.repository.ReportRepository
import javax.inject.Inject

/** 掲示板投稿・ユーザープロフィールの通報 */
class SubmitReportUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(
        targetType: ReportTargetType,
        targetId: String,
        reasonCategory: ReportReasonCategory,
        reasonText: String?
    ): Report = reportRepository.submitReport(targetType, targetId, reasonCategory, reasonText)
}
