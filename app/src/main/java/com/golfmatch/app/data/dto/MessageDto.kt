package com.golfmatch.app.data.dto

import com.google.gson.annotations.SerializedName

/** メッセージDTO（技術設計書 6-7章、ADR-0004） */
data class MessageDto(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("user_a_id") val userAId: String,
    @SerializedName("user_b_id") val userBId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("content") val content: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("read_at") val readAt: String?
)

/** `POST /conversations/{partnerId}/messages` リクエストボディ */
data class SendMessageRequestDto(
    @SerializedName("content") val content: String
)
