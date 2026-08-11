package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.AuthSessionResponseDto
import com.golfmatch.app.data.dto.VerifyOtpResponseDto
import com.golfmatch.app.domain.model.AuthSession
import com.golfmatch.app.domain.model.PhoneOtpVerificationResult
import com.golfmatch.app.domain.model.RegistrationToken

/**
 * `POST /auth/phone/verify` レスポンスをドメインモデルに変換する（ADR-0006）。
 *
 * `is_new_user`のみを分岐の判定材料とする。`false`（既存ユーザー）側では、既存の
 * `AuthSessionResponseDto.toDomain()`（`user`必須・null時例外送出、ADR-0005）をそのまま再利用する。
 */
fun VerifyOtpResponseDto.toDomain(): PhoneOtpVerificationResult =
    if (isNewUser) {
        val token = checkNotNull(registrationToken) {
            "VerifyOtpResponseDto.registration_token must not be null when is_new_user=true (契約違反)"
        }
        PhoneOtpVerificationResult.NewUser(RegistrationToken(value = token))
    } else {
        val sessionDto = checkNotNull(session) {
            "VerifyOtpResponseDto.session must not be null when is_new_user=false (契約違反)"
        }
        PhoneOtpVerificationResult.ExistingUser(sessionDto.toDomain())
    }

/**
 * `POST /users` / `POST /auth/phone/verify`（既存ユーザー分岐）レスポンスをドメインモデルに変換する。
 *
 * `user` は契約上（技術設計書6-1章、ADR-0005）どちらのレスポンスでも必須。
 * サーバー実装の不備等で `user` が欠落していた場合、`userId` を空文字列へ
 * サイレントにフォールバックさせると「ログインできたのに自分が誰か分からない」
 * 状態のまま後続処理に伝播してしまうため、契約違反として早期に例外を送出する。
 */
fun AuthSessionResponseDto.toDomain(): AuthSession {
    val user = checkNotNull(user) {
        "AuthSessionResponseDto.user must not be null (POST /users, POST /auth/phone/verify のレスポンス契約違反)"
    }
    return AuthSession(
        accessToken = accessToken,
        userId = user.userId
    )
}
