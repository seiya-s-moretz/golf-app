package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.RegistrationToken
import com.golfmatch.app.domain.repository.AuthRepository
import javax.inject.Inject

/** OTP検証。成功時、本登録に進むための一時トークンを返す（ADR-0003） */
class VerifyPhoneOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phoneNumber: String, otpCode: String): RegistrationToken =
        authRepository.verifyPhoneOtp(phoneNumber, otpCode)
}
