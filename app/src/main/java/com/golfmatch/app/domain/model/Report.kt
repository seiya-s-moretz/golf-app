package com.golfmatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * 通報（技術設計書 5-2章）
 *
 * ステータス更新（PENDING/REVIEWING/RESOLVED/DISMISSED）は通報管理（簡易管理画面、
 * `User.is_admin=true`の運営メンバー向け）から行う（技術設計書6-9章、ADR-0007）。
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
    val handledByUserId: String?,
    val handledAt: Instant?,
    val handlingMemo: String?
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

/**
 * 通報の対応ステータス（技術設計書5-2章、ADR-0007で`REVIEWED`→`REVIEWING`、`ACTION_TAKEN`→`RESOLVED`に改称）。
 *
 * MVPでは状態遷移順序の強制は行わない（過剰設計を避ける方針、技術設計書6-9章）。
 */
enum class ReportStatus {
    PENDING,
    REVIEWING,
    RESOLVED,
    DISMISSED
}

/**
 * 通報管理一覧画面向けのサマリ（技術設計書6-9章 `GET /admin/reports` レスポンス相当）。
 *
 * 一覧表示に必要な通報者・通報対象の要約を1回の呼び出しで取得できるようにするため、
 * [Report] 本体とは別に一覧専用の型として保持する。
 */
data class ReportSummary(
    val report: Report,
    val reporterName: String,
    val reporterIconUrl: String,
    /** USERなら対象ユーザーの氏名、BOARD_POSTなら投稿者氏名・本文冒頭（技術設計書6-9章） */
    val targetSummary: String
)

/**
 * 通報管理詳細画面向けの詳細情報（技術設計書6-9章 `GET /admin/reports/{id}` レスポンス相当）。
 *
 * 通報対象が[ReportTargetType.USER]の場合は[targetUser]、[ReportTargetType.BOARD_POST]の場合は
 * [targetBoardPost]が非nullとなる（[Report.targetType]で判定）。USERの場合も`phone_number`等の
 * 機微情報は含めない（技術設計書8章の非機微情報方針、6-9章）。
 */
data class ReportDetail(
    val report: Report,
    val reporterName: String,
    val reporterIconUrl: String,
    val targetUser: ReportTargetUserDetail? = null,
    val targetBoardPost: ReportTargetBoardPostDetail? = null
)

data class ReportTargetUserDetail(
    val userId: String,
    val name: String,
    val iconUrl: String,
    val gender: String,
    val age: Int,
    val introduction: String
)

data class ReportTargetBoardPostDetail(
    val postId: String,
    val authorUserId: String,
    val authorName: String,
    val content: String
)
