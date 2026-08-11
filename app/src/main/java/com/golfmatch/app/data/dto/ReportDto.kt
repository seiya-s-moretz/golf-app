package com.golfmatch.app.data.dto

import com.google.gson.annotations.SerializedName

/** 通報DTO（技術設計書 6-8章） */
data class ReportDto(
    @SerializedName("report_id") val reportId: String,
    @SerializedName("reporter_user_id") val reporterUserId: String,
    @SerializedName("target_type") val targetType: String,
    @SerializedName("target_id") val targetId: String,
    @SerializedName("reason_category") val reasonCategory: String,
    @SerializedName("reason_text") val reasonText: String?,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("handled_by_user_id") val handledByUserId: String?,
    @SerializedName("handled_at") val handledAt: String?,
    @SerializedName("handling_memo") val handlingMemo: String?
)

/** `POST /reports` リクエストボディ */
data class SubmitReportRequestDto(
    @SerializedName("target_type") val targetType: String,
    @SerializedName("target_id") val targetId: String,
    @SerializedName("reason_category") val reasonCategory: String,
    @SerializedName("reason_text") val reasonText: String?
)

/** 通報管理API（管理者向け）で共通利用する通報者の要約DTO（技術設計書6-9章） */
data class ReportAdminReporterDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon_url") val iconUrl: String
)

/**
 * `GET /admin/reports` の一覧要素DTO（技術設計書6-9章）。
 *
 * [ReportDto] の全項目に加え、一覧表示に必要な通報者[reporter]・通報対象の要約[targetSummary]を含む。
 */
data class ReportAdminSummaryDto(
    @SerializedName("report_id") val reportId: String,
    @SerializedName("reporter_user_id") val reporterUserId: String,
    @SerializedName("target_type") val targetType: String,
    @SerializedName("target_id") val targetId: String,
    @SerializedName("reason_category") val reasonCategory: String,
    @SerializedName("reason_text") val reasonText: String?,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("handled_by_user_id") val handledByUserId: String?,
    @SerializedName("handled_at") val handledAt: String?,
    @SerializedName("handling_memo") val handlingMemo: String?,
    @SerializedName("reporter") val reporter: ReportAdminReporterDto,
    @SerializedName("target_summary") val targetSummary: String
)

/** `GET /admin/reports/{id}` の通報対象詳細DTO（技術設計書6-9章）。target_typeにより一方のみ非null */
data class ReportAdminTargetDetailDto(
    @SerializedName("user") val user: ReportAdminTargetUserDto?,
    @SerializedName("board_post") val boardPost: ReportAdminTargetBoardPostDto?
)

data class ReportAdminTargetUserDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon_url") val iconUrl: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("age") val age: Int,
    @SerializedName("introduction") val introduction: String
)

data class ReportAdminTargetBoardPostDto(
    @SerializedName("post_id") val postId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("author_name") val authorName: String,
    @SerializedName("content") val content: String
)

/** `GET /admin/reports/{id}` レスポンスDTO（技術設計書6-9章） */
data class ReportAdminDetailDto(
    @SerializedName("report_id") val reportId: String,
    @SerializedName("reporter_user_id") val reporterUserId: String,
    @SerializedName("target_type") val targetType: String,
    @SerializedName("target_id") val targetId: String,
    @SerializedName("reason_category") val reasonCategory: String,
    @SerializedName("reason_text") val reasonText: String?,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("handled_by_user_id") val handledByUserId: String?,
    @SerializedName("handled_at") val handledAt: String?,
    @SerializedName("handling_memo") val handlingMemo: String?,
    @SerializedName("reporter") val reporter: ReportAdminReporterDto,
    @SerializedName("target_detail") val targetDetail: ReportAdminTargetDetailDto
)

/** `PATCH /admin/reports/{id}/status` リクエストボディ（技術設計書6-9章） */
data class UpdateReportStatusRequestDto(
    @SerializedName("status") val status: String,
    @SerializedName("handling_memo") val handlingMemo: String?
)
