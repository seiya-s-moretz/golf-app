package com.golfmatch.app.data.dto

import com.google.gson.annotations.SerializedName

/** 会話一覧DTO（`GET /conversations` レスポンス、技術設計書 6-7章） */
data class ConversationDto(
    @SerializedName("partner") val partner: UserDto,
    @SerializedName("last_message") val lastMessage: MessageDto?,
    @SerializedName("unread_count") val unreadCount: Int,
    @SerializedName("updated_at") val updatedAt: String
)
