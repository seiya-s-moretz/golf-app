package com.golfmatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * 電話番号認証（技術設計書 5-2章、ADR-0003）
 *
 * OTPコードのハッシュ値はサーバー側のみが保持し、クライアントの[domain]層には含めない。
 */
data class PhoneVerification(
    val verificationId: String,
    val phoneNumber: String,
    val status: PhoneVerificationStatus,
    val expiresAt: Instant,
    val attemptCount: Int,
    val createdAt: Instant,
    val verifiedAt: Instant?
)

enum class PhoneVerificationStatus {
    PENDING,
    VERIFIED,
    EXPIRED,
    FAILED
}
