package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.MatchRequestDto
import com.golfmatch.app.domain.model.MatchRequest
import com.golfmatch.app.domain.model.MatchRequestStatus
import kotlinx.datetime.Instant

fun MatchRequestDto.toDomain(): MatchRequest = MatchRequest(
    matchRequestId = matchRequestId,
    fromUserId = fromUserId,
    toUserId = toUserId,
    status = MatchRequestStatus.valueOf(status),
    createdAt = Instant.parse(createdAt),
    respondedAt = respondedAt?.let { Instant.parse(it) }
)
