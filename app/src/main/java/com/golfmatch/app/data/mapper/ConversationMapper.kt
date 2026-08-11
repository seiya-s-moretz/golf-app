package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.ConversationDto
import com.golfmatch.app.domain.model.Conversation
import kotlinx.datetime.Instant

fun ConversationDto.toDomain(): Conversation = Conversation(
    partner = partner.toDomain(),
    lastMessage = lastMessage?.toDomain(),
    unreadCount = unreadCount,
    updatedAt = Instant.parse(updatedAt)
)
