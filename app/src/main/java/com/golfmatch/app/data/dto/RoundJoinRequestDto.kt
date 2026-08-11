package com.golfmatch.app.data.dto

import com.google.gson.annotations.SerializedName

/** ラウンド参加申請DTO（技術設計書 6-4章、ADR-0001） */
data class RoundJoinRequestDto(
    @SerializedName("join_request_id") val joinRequestId: String,
    @SerializedName("event_id") val eventId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("responded_at") val respondedAt: String?
)
