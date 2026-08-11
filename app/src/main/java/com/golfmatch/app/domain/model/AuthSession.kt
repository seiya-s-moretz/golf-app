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
