package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.AuthSessionResponseDto
import com.golfmatch.app.data.dto.VerifyOtpResponseDto
import com.golfmatch.app.domain.model.AuthSession
import com.golfmatch.app.domain.model.RegistrationToken

fun VerifyOtpResponseDto.toDomain(): RegistrationToken = RegistrationToken(value = registrationToken)

fun AuthSessionResponseDto.toDomain(): AuthSession = AuthSession(
    accessToken = accessToken,
    userId = user?.userId.orEmpty()
)
