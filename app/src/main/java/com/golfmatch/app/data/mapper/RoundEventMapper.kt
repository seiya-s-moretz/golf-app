package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.RoundEventDto
import com.golfmatch.app.domain.model.RoundEvent
import kotlinx.datetime.Instant

fun RoundEventDto.toDomain(): RoundEvent = RoundEvent(
    eventId = eventId,
    clubName = clubName,
    datetime = Instant.parse(datetime),
    fee = fee,
    capacity = capacity,
    current = current,
    createdBy = createdBy,
    createdAt = Instant.parse(createdAt)
)
