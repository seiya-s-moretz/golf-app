package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.Conversation
import com.golfmatch.app.domain.repository.MessageRepository
import javax.inject.Inject

/** 自分が関わる会話一覧の取得（Connectionが存在するユーザーペア単位、ADR-0004） */
class GetConversationsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(): List<Conversation> = messageRepository.getConversations()
}
