package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.AreaDto
import com.golfmatch.app.data.dto.ConversationDto
import com.golfmatch.app.data.dto.MessageDto
import com.golfmatch.app.data.dto.UserDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationMapperTest {

    private fun userDto(userId: String) = UserDto(
        userId = userId,
        name = "相手ユーザー",
        iconUrl = "",
        gender = "female",
        age = 28,
        area = AreaDto("area-1", "埼玉県", "さいたま市", 1, true, "2026-01-01T00:00:00Z"),
        averageScore = 95,
        purpose = "CASUAL",
        introduction = "",
        phoneNumber = "+819000000000",
        phoneVerified = true,
        phoneVerifiedAt = "2026-01-01T00:00:00Z",
        status = "ACTIVE",
        createdAt = "2026-01-01T00:00:00Z"
    )

    @Test
    fun `会話一覧DTOは相手ユーザー・最新メッセージ・未読数・更新日時が正しく変換される(技術設計書6-7章)`() {
        val dto = ConversationDto(
            partner = userDto("user-2"),
            lastMessage = MessageDto(
                messageId = "message-1",
                userAId = "user-1",
                userBId = "user-2",
                senderId = "user-2",
                content = "よろしくお願いします",
                createdAt = "2026-08-01T00:00:00Z",
                readAt = null
            ),
            unreadCount = 3,
            updatedAt = "2026-08-01T00:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals("user-2", domain.partner.userId)
        assertEquals("よろしくお願いします", domain.lastMessage?.content)
        assertEquals(3, domain.unreadCount)
    }

    @Test
    fun `lastMessageがnull(まだメッセージがない会話)でも変換できる`() {
        val dto = ConversationDto(
            partner = userDto("user-2"),
            lastMessage = null,
            unreadCount = 0,
            updatedAt = "2026-08-01T00:00:00Z"
        )

        val domain = dto.toDomain()

        assertNull(domain.lastMessage)
        assertEquals(0, domain.unreadCount)
    }
}
