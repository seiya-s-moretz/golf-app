package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.MessageDto
import com.golfmatch.app.domain.model.Message
import kotlinx.datetime.Instant

fun MessageDto.toDomain(): Message = Message(
    messageId = messageId,
    userAId = userAId,
    userBId = userBId,
    senderId = senderId,
    content = content,
    createdAt = Instant.parse(createdAt),
    readAt = readAt?.let { Instant.parse(it) }
)
