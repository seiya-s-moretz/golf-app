package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.Message
import com.golfmatch.app.domain.repository.MessageRepository
import javax.inject.Inject

/** 特定の相手とのメッセージ履歴取得（ページネーション対応） */
class GetMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(partnerId: String, before: String? = null, limit: Int = 50): List<Message> =
        messageRepository.getMessages(partnerId, before, limit)
}
