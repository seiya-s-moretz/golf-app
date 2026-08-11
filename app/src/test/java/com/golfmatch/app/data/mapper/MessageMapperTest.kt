package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.MessageDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class MessageMapperTest {

    @Test
    fun `未読メッセージはreadAtがnullのまま変換される(技術設計書5-2章 Message)`() {
        val dto = MessageDto(
            messageId = "message-1",
            userAId = "user-1",
            userBId = "user-2",
            senderId = "user-1",
            content = "今度よろしくお願いします",
            createdAt = "2026-08-01T00:00:00Z",
            readAt = null
        )

        val domain = dto.toDomain()

        assertEquals("message-1", domain.messageId)
        assertEquals("user-1", domain.userAId)
        assertEquals("user-2", domain.userBId)
        assertEquals("user-1", domain.senderId)
        assertEquals("今度よろしくお願いします", domain.content)
        assertNull(domain.readAt)
    }

    @Test
    fun `既読メッセージはreadAtが変換される`() {
        val dto = MessageDto(
            messageId = "message-1",
            userAId = "user-1",
            userBId = "user-2",
            senderId = "user-2",
            content = "了解です",
            createdAt = "2026-08-01T00:00:00Z",
            readAt = "2026-08-01T01:00:00Z"
        )

        assertNotNull(dto.toDomain().readAt)
    }
}
