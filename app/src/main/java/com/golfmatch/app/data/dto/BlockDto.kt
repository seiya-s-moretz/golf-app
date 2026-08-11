package com.golfmatch.app.data.dto

import com.google.gson.annotations.SerializedName

/** ブロックDTO（技術設計書 5-2章、6-3章） */
data class BlockDto(
    @SerializedName("block_id") val blockId: String,
    @SerializedName("blocker_user_id") val blockerUserId: String,
    @SerializedName("blocked_user_id") val blockedUserId: String,
    @SerializedName("created_at") val createdAt: String
)
