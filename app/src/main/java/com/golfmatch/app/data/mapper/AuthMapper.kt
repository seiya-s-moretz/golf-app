package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.AuthSessionResponseDto
import com.golfmatch.app.data.dto.VerifyOtpResponseDto
import com.golfmatch.app.domain.model.AuthSession
import com.golfmatch.app.domain.model.RegistrationToken

fun VerifyOtpResponseDto.toDomain(): RegistrationToken = RegistrationToken(value = registrationToken)

/**
 * `POST /users` / `POST /auth/login` レスポンスをドメインモデルに変換する。
 *
 * `user` は契約上（技術設計書6-1章、ADR-0005）どちらのレスポンスでも必須。
 * サーバー実装の不備等で `user` が欠落していた場合、`userId` を空文字列へ
 * サイレントにフォールバックさせると「ログインできたのに自分が誰か分からない」
 * 状態のまま後続処理に伝播してしまうため、契約違反として早期に例外を送出する。
 */
fun AuthSessionResponseDto.toDomain(): AuthSession {
    val user = checkNotNull(user) {
        "AuthSessionResponseDto.user must not be null (POST /users, POST /auth/login のレスポンス契約違反)"
    }
    return AuthSession(
        accessToken = accessToken,
        userId = user.userId
    )
}
