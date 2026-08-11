package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.RoundJoinRequestDto
import com.golfmatch.app.domain.model.RoundJoinRequest
import com.golfmatch.app.domain.model.RoundJoinRequestStatus
import kotlinx.datetime.Instant

fun RoundJoinRequestDto.toDomain(): RoundJoinRequest = RoundJoinRequest(
    joinRequestId = joinRequestId,
    eventId = eventId,
    userId = userId,
    status = RoundJoinRequestStatus.valueOf(status),
    createdAt = Instant.parse(createdAt),
    respondedAt = respondedAt?.let { Instant.parse(it) }
)
