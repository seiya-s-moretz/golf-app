package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.ReportDto
import com.golfmatch.app.domain.model.Report
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.model.ReportTargetType
import kotlinx.datetime.Instant

fun ReportDto.toDomain(): Report = Report(
    reportId = reportId,
    reporterUserId = reporterUserId,
    targetType = ReportTargetType.valueOf(targetType),
    targetId = targetId,
    reasonCategory = ReportReasonCategory.valueOf(reasonCategory),
    reasonText = reasonText,
    status = ReportStatus.valueOf(status),
    createdAt = Instant.parse(createdAt),
    reviewedAt = reviewedAt?.let { Instant.parse(it) }
)
