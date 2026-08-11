package com.golfmatch.app.data.dto

import com.google.gson.annotations.SerializedName

/** `POST /auth/phone/otp` リクエストボディ（技術設計書 6-1章、ADR-0003） */
data class RequestOtpRequestDto(
    @SerializedName("phone_number") val phoneNumber: String
)

/** `POST /auth/phone/verify` リクエストボディ */
data class VerifyOtpRequestDto(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("otp_code") val otpCode: String
)

/** `POST /auth/phone/verify` レスポンス */
data class VerifyOtpResponseDto(
    @SerializedName("registration_token") val registrationToken: String
)

/** `POST /users`（新規登録）リクエストボディ */
data class RegisterUserRequestDto(
    @SerializedName("registration_token") val registrationToken: String,
    @SerializedName("name") val name: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("age") val age: Int,
    @SerializedName("area_id") val areaId: String,
    @SerializedName("average_score") val averageScore: Int,
    @SerializedName("purpose") val purpose: String,
    @SerializedName("introduction") val introduction: String
)

/** `POST /auth/login` リクエストボディ */
data class LoginRequestDto(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("otp_code") val otpCode: String
)

/** `POST /users`, `POST /auth/login` レスポンス（アカウント作成/ログイン結果） */
data class AuthSessionResponseDto(
    @SerializedName("user") val user: UserDto?,
    @SerializedName("access_token") val accessToken: String
)
