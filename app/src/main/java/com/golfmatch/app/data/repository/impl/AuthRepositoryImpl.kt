package com.golfmatch.app.data.repository.impl

import com.golfmatch.app.data.api.ApiService
import com.golfmatch.app.data.auth.AuthSessionManager
import com.golfmatch.app.data.dto.RegisterUserRequestDto
import com.golfmatch.app.data.dto.RequestOtpRequestDto
import com.golfmatch.app.data.dto.VerifyOtpRequestDto
import com.golfmatch.app.data.mapper.toDomain
import com.golfmatch.app.domain.model.AuthSession
import com.golfmatch.app.domain.model.PhoneOtpVerificationResult
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val sessionManager: AuthSessionManager
) : AuthRepository {

    override suspend fun requestPhoneOtp(phoneNumber: String) =
        api.requestPhoneOtp(RequestOtpRequestDto(phoneNumber))

    override suspend fun verifyPhoneOtp(phoneNumber: String, otpCode: String): PhoneOtpVerificationResult =
        api.verifyPhoneOtp(VerifyOtpRequestDto(phoneNumber, otpCode)).toDomain().also { result ->
            if (result is PhoneOtpVerificationResult.ExistingUser) {
                sessionManager.updateSession(result.session.accessToken, result.session.userId)
            }
        }

    override suspend fun registerUser(
        registrationToken: String,
        name: String,
        gender: String,
        age: Int,
        areaId: String,
        averageScore: Int,
        purpose: Purpose,
        introduction: String
    ): AuthSession = api.registerUser(
        RegisterUserRequestDto(
            registrationToken = registrationToken,
            name = name,
            gender = gender,
            age = age,
            areaId = areaId,
            averageScore = averageScore,
            purpose = purpose.name,
            introduction = introduction
        )
    ).toDomain().also { sessionManager.updateSession(it.accessToken, it.userId) }
}
