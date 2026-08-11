package com.golfmatch.app.data.dto

import com.google.gson.annotations.SerializedName

/** マッチング申請DTO（技術設計書 6-5章） */
data class MatchRequestDto(
    @SerializedName("match_request_id") val matchRequestId: String,
    @SerializedName("from_user_id") val fromUserId: String,
    @SerializedName("to_user_id") val toUserId: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("responded_at") val respondedAt: String?
)
