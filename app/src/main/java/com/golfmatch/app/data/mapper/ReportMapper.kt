package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.ReportAdminDetailDto
import com.golfmatch.app.data.dto.ReportAdminSummaryDto
import com.golfmatch.app.data.dto.ReportDto
import com.golfmatch.app.domain.model.Report
import com.golfmatch.app.domain.model.ReportDetail
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.model.ReportSummary
import com.golfmatch.app.domain.model.ReportTargetBoardPostDetail
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.domain.model.ReportTargetUserDetail
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
    handledByUserId = handledByUserId,
    handledAt = handledAt?.let { Instant.parse(it) },
    handlingMemo = handlingMemo
)

fun ReportAdminSummaryDto.toDomain(): ReportSummary = ReportSummary(
    report = Report(
        reportId = reportId,
        reporterUserId = reporterUserId,
        targetType = ReportTargetType.valueOf(targetType),
        targetId = targetId,
        reasonCategory = ReportReasonCategory.valueOf(reasonCategory),
        reasonText = reasonText,
        status = ReportStatus.valueOf(status),
        createdAt = Instant.parse(createdAt),
        handledByUserId = handledByUserId,
        handledAt = handledAt?.let { Instant.parse(it) },
        handlingMemo = handlingMemo
    ),
    reporterName = reporter.name,
    reporterIconUrl = reporter.iconUrl,
    targetSummary = targetSummary
)

fun ReportAdminDetailDto.toDomain(): ReportDetail = ReportDetail(
    report = Report(
        reportId = reportId,
        reporterUserId = reporterUserId,
        targetType = ReportTargetType.valueOf(targetType),
        targetId = targetId,
        reasonCategory = ReportReasonCategory.valueOf(reasonCategory),
        reasonText = reasonText,
        status = ReportStatus.valueOf(status),
        createdAt = Instant.parse(createdAt),
        handledByUserId = handledByUserId,
        handledAt = handledAt?.let { Instant.parse(it) },
        handlingMemo = handlingMemo
    ),
    reporterName = reporter.name,
    reporterIconUrl = reporter.iconUrl,
    targetUser = targetDetail.user?.let {
        ReportTargetUserDetail(
            userId = it.userId,
            name = it.name,
            iconUrl = it.iconUrl,
            gender = it.gender,
            age = it.age,
            introduction = it.introduction
        )
    },
    targetBoardPost = targetDetail.boardPost?.let {
        ReportTargetBoardPostDetail(
            postId = it.postId,
            authorUserId = it.userId,
            authorName = it.authorName,
            content = it.content
        )
    }
)
