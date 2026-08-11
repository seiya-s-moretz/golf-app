package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.BoardPostDto
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class BoardPostMapperTest {

    @Test
    fun `BoardPostDtoをtoDomainすると全フィールドが正しく変換される`() {
        val dto = BoardPostDto(
            postId = "post-1",
            userId = "user-1",
            content = "本日ハーフ48で回れました",
            createdAt = "2026-08-01T00:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals("post-1", domain.postId)
        assertEquals("user-1", domain.userId)
        assertEquals("本日ハーフ48で回れました", domain.content)
        assertEquals(Instant.parse("2026-08-01T00:00:00Z"), domain.createdAt)
    }
}
