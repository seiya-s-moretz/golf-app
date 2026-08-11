package com.golfmatch.app.domain.repository

import com.golfmatch.app.domain.model.Report
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportTargetType

/**
 * 通報のリポジトリ（技術設計書 6-8章）
 */
interface ReportRepository {
    suspend fun submitReport(
        targetType: ReportTargetType,
        targetId: String,
        reasonCategory: ReportReasonCategory,
        reasonText: String?
    ): Report
}
