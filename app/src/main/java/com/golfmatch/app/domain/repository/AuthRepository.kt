package com.golfmatch.app.domain.repository

import com.golfmatch.app.domain.model.AuthSession
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.model.RegistrationToken

/**
 * 認証・本人確認のリポジトリ（技術設計書 6-1章、ADR-0003）
 */
interface AuthRepository {
    /** SMSでOTPを送信する。認証不要 */
    suspend fun requestPhoneOtp(phoneNumber: String)

    /** OTPを検証し、本登録に進むための一時トークンを取得する。認証不要 */
    suspend fun verifyPhoneOtp(phoneNumber: String, otpCode: String): RegistrationToken

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

    /** 既存ユーザーの再ログイン（電話番号 + OTP方式を再利用） */
    suspend fun login(phoneNumber: String, otpCode: String): AuthSession
}
