package com.golfmatch.app.domain.model

/**
 * 認証セッション（技術設計書 2章・6-1章、ADR-0003）
 *
 * アカウント作成・ログイン成功時に発行されるBearerトークンを保持する。
 * クライアントは全APIリクエストに `Authorization: Bearer <accessToken>` を付与する。
 */
data class AuthSession(
    val accessToken: String,
    val userId: String
)

/**
 * OTP検証成功後、`POST /users` による本登録までの間だけ有効な一時トークン（技術設計書 6-1章）。
 * アカウント未作成の状態でのみ使用される。
 */
data class RegistrationToken(
    val value: String
)

/**
 * `POST /auth/phone/verify` の検証結果（技術設計書6-1章、ADR-0006）。
 *
 * OTP検証成功後、新規/既存ユーザーいずれかの結果を確定的に表す。
 * [ExistingUser]の場合は認証（セッション開始）まで完了しており、[NewUser]の場合は
 * `POST /users` による本登録に進む必要がある。
 */
sealed interface PhoneOtpVerificationResult {
    data class ExistingUser(val session: AuthSession) : PhoneOtpVerificationResult
    data class NewUser(val registrationToken: RegistrationToken) : PhoneOtpVerificationResult
}
