package com.golfmatch.app.domain.repository

import com.golfmatch.app.domain.model.AuthSession
import com.golfmatch.app.domain.model.PhoneOtpVerificationResult
import com.golfmatch.app.domain.model.Purpose

/**
 * 認証・本人確認のリポジトリ（技術設計書 6-1章、ADR-0003、ADR-0006）
 */
interface AuthRepository {
    /** SMSでOTPを送信する。認証不要 */
    suspend fun requestPhoneOtp(phoneNumber: String)

    /**
     * OTPを検証する。新規/既存ユーザーの判定・認証完了までを1回で行う（ADR-0006）。
     * 既存ユーザーの場合はこの呼び出し内でセッションを開始する。
     */
    suspend fun verifyPhoneOtp(phoneNumber: String, otpCode: String): PhoneOtpVerificationResult

    /** プロフィール初回登録とアカウント作成を兼ねる */
    suspend fun registerUser(
        registrationToken: String,
        name: String,
        gender: String,
        age: Int,
        areaId: String,
        averageScore: Int,
        purpose: Purpose,
        introduction: String
    ): AuthSession
}
