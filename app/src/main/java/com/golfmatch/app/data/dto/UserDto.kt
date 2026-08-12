package com.golfmatch.app.data.dto

import com.google.gson.annotations.SerializedName

/**
 * ユーザーDTO（技術設計書 6-3章 `GET/PUT /users/{id}` レスポンス）
 *
 * 日時はISO-8601文字列で受け渡す。areaはAreaMasterの参照展開（技術設計書 6-3章）。
 */
data class UserDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("icon_url") val iconUrl: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("age") val age: Int,
    @SerializedName("area") val area: AreaDto,
    @SerializedName("average_score") val averageScore: Int,
    @SerializedName("purpose") val purpose: String,
    @SerializedName("introduction") val introduction: String,
    // 他人閲覧時はPII保護のためサーバーがフィールド自体を省略する（本人閲覧時のみ非null）
    @SerializedName("phone_number") val phoneNumber: String?,
    @SerializedName("phone_verified") val phoneVerified: Boolean,
    @SerializedName("phone_verified_at") val phoneVerifiedAt: String?,
    @SerializedName("status") val status: String,
    @SerializedName("is_admin") val isAdmin: Boolean = false,
    @SerializedName("created_at") val createdAt: String
)

/** `PUT /users/{id}` リクエストボディ */
data class UpdateUserRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("age") val age: Int,
    @SerializedName("area_id") val areaId: String,
    @SerializedName("average_score") val averageScore: Int,
    @SerializedName("purpose") val purpose: String,
    @SerializedName("introduction") val introduction: String
)
