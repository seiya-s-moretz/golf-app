package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.Message
import com.golfmatch.app.domain.repository.MessageRepository
import javax.inject.Inject

/** メッセージ送信（Connectionが存在しない、またはブロック関係にある場合はサーバー側で拒否） */
class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(partnerId: String, content: String): Message =
        messageRepository.sendMessage(partnerId, content)
}
