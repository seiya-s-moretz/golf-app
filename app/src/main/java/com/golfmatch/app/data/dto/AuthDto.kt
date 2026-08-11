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

/**
 * `POST /auth/phone/verify` レスポンス（技術設計書6-1章、ADR-0006）。
 *
 * `is_new_user` がクライアントの分岐に用いる唯一の確定的な判定材料（ADR-0006の原則）。
 * `is_new_user=false`（既存ユーザー）の場合のみ `session` が非null、
 * `is_new_user=true`（新規ユーザー）の場合のみ `registration_token` が非null となる。
 * 他フィールドの有無による暗黙の分岐は行わない。
 */
data class VerifyOtpResponseDto(
    @SerializedName("is_new_user") val isNewUser: Boolean,
    @SerializedName("session") val session: AuthSessionResponseDto?,
    @SerializedName("registration_token") val registrationToken: String?
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

/**
 * `POST /users`, `POST /auth/phone/verify`（既存ユーザー分岐） レスポンス（アカウント作成/認証結果）。
 *
 * `user` は業務上、いずれのレスポンスにも常に含まれる（技術設計書6-1章、ADR-0005）。
 * 型が nullable なのはJSONパース時の防御的措置に過ぎず、業務上 `user` が null であることは
 * サーバー側の契約違反を意味する異常系である（`AuthMapper.toDomain()` は null の場合に例外を送出する）。
 */
data class AuthSessionResponseDto(
    @SerializedName("user") val user: UserDto?,
    @SerializedName("access_token") val accessToken: String
)
