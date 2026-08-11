package com.golfmatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * 通報（技術設計書 5-2章）
 *
 * ステータス更新（PENDING→REVIEWED等）を行う管理画面・管理APIは本アプリのスコープ外。
 * MVPでは運営による手動確認（DB直接操作等）を想定する。
 */
data class Report(
    val reportId: String,
    val reporterUserId: String,
    val targetType: ReportTargetType,
    val targetId: String,
    val reasonCategory: ReportReasonCategory,
    val reasonText: String?,
    val status: ReportStatus,
    val createdAt: Instant,
    val reviewedAt: Instant?
)

enum class ReportTargetType {
    USER,
    BOARD_POST
}

/** DATING_SOLICITATION（出会い目的利用）はPRD「恋愛・出会い目的での利用禁止」方針を具体化した理由 */
enum class ReportReasonCategory {
    SPAM,
    DATING_SOLICITATION,
    HARASSMENT,
    INAPPROPRIATE_CONTENT,
    OTHER
}

enum class ReportStatus {
    PENDING,
    REVIEWED,
    ACTION_TAKEN,
    DISMISSED
}
