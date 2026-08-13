package com.golfmatch.app.data.repository.impl

import com.golfmatch.app.data.api.ApiService
import com.golfmatch.app.data.dto.SendMessageRequestDto
import com.golfmatch.app.data.mapper.toDomain
import com.golfmatch.app.domain.model.Conversation
import com.golfmatch.app.domain.model.Message
import com.golfmatch.app.domain.repository.MessageRepository
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val api: ApiService
) : MessageRepository {

    override suspend fun getConversations(before: String?, beforeId: String?, limit: Int): List<Conversation> =
        api.getConversations(before, beforeId, limit).map { it.toDomain() }

    override suspend fun getMessages(
        partnerId: String,
        before: String?,
        beforeId: String?,
        limit: Int
    ): List<Message> = api.getMessages(partnerId, before, beforeId, limit).map { it.toDomain() }

    override suspend fun sendMessage(partnerId: String, content: String): Message =
        api.sendMessage(partnerId, SendMessageRequestDto(content)).toDomain()

    override suspend fun markAsRead(partnerId: String) = api.markConversationAsRead(partnerId)
}
