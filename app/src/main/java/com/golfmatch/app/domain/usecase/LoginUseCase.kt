package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.AuthSession
import com.golfmatch.app.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * 既存ユーザーの再ログイン（電話番号 + OTP方式を再利用、技術設計書6-1章、ADR-0003）。
 *
 * 他のAuthRepository系メソッドと同様、既存パターンに揃えてUseCaseとして薄くラップする
 * （[OtpVerificationViewModel][com.golfmatch.app.ui.viewmodel.OtpVerificationViewModel]の
 * 新規/既存ユーザー分岐で利用。詳細は同ViewModelのKDoc参照）。
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phoneNumber: String, otpCode: String): AuthSession =
        authRepository.login(phoneNumber, otpCode)
}
