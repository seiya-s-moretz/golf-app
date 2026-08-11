package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.PhoneOtpVerificationResult
import com.golfmatch.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * OTP検証。新規/既存ユーザーの判定・認証完了までを1回で行う（ADR-0006）。
 * 既存ユーザーなら認証完了済みの[PhoneOtpVerificationResult.ExistingUser]、
 * 新規ユーザーなら本登録用トークンを含む[PhoneOtpVerificationResult.NewUser]を返す。
 */
class VerifyPhoneOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phoneNumber: String, otpCode: String): PhoneOtpVerificationResult =
        authRepository.verifyPhoneOtp(phoneNumber, otpCode)
}
