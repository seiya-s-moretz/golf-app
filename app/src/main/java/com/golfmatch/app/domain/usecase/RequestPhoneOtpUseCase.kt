package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.repository.AuthRepository
import javax.inject.Inject

/** 電話番号入力後、SMSでOTPを送信する（ADR-0003） */
class RequestPhoneOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phoneNumber: String) = authRepository.requestPhoneOtp(phoneNumber)
}
