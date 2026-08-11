package com.golfmatch.app.data.dto

import com.google.gson.annotations.SerializedName

/** ラウンド募集DTO（技術設計書 6-4章） */
data class RoundEventDto(
    @SerializedName("event_id") val eventId: String,
    @SerializedName("club_name") val clubName: String,
    @SerializedName("datetime") val datetime: String,
    @SerializedName("fee") val fee: Int,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("current") val current: Int,
    @SerializedName("created_by") val createdBy: String,
    @SerializedName("created_at") val createdAt: String
)

/** `POST /round-events` リクエストボディ */
data class CreateRoundEventRequestDto(
    @SerializedName("club_name") val clubName: String,
    @SerializedName("datetime") val datetime: String,
    @SerializedName("fee") val fee: Int,
    @SerializedName("capacity") val capacity: Int
)
