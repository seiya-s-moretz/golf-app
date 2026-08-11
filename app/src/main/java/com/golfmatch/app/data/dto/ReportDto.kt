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
    @SerializedName("reviewed_at") val reviewedAt: String?
)

/** `POST /reports` リクエストボディ */
data class SubmitReportRequestDto(
    @SerializedName("target_type") val targetType: String,
    @SerializedName("target_id") val targetId: String,
    @SerializedName("reason_category") val reasonCategory: String,
    @SerializedName("reason_text") val reasonText: String?
)
