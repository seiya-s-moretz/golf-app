package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.UserDto
import com.golfmatch.app.domain.model.AccountStatus
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.model.User
import kotlinx.datetime.Instant

fun UserDto.toDomain(): User = User(
    userId = userId,
    name = name,
    iconUrl = iconUrl,
    gender = gender,
    age = age,
    areaId = area.areaId,
    averageScore = averageScore,
    purpose = Purpose.fromWireValue(purpose),
    introduction = introduction,
    phoneNumber = phoneNumber,
    phoneVerified = phoneVerified,
    phoneVerifiedAt = phoneVerifiedAt?.let { Instant.parse(it) },
    status = AccountStatus.valueOf(status),
    isAdmin = isAdmin,
    createdAt = Instant.parse(createdAt)
)
