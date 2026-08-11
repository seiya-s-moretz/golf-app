package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.MatchRequestDto
import com.golfmatch.app.domain.model.MatchRequestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchRequestMapperTest {

    @Test
    fun `PENDING状態ではrespondedAtがnull`() {
        val dto = MatchRequestDto(
            matchRequestId = "match-1",
            fromUserId = "user-1",
            toUserId = "user-2",
            status = "PENDING",
            createdAt = "2026-08-01T00:00:00Z",
            respondedAt = null
        )

        val domain = dto.toDomain()

        assertEquals("match-1", domain.matchRequestId)
        assertEquals("user-1", domain.fromUserId)
        assertEquals("user-2", domain.toUserId)
        assertEquals(MatchRequestStatus.PENDING, domain.status)
        assertNull(domain.respondedAt)
    }

    @Test
    fun `ACCEPTED状態が正しく変換される`() {
        val dto = MatchRequestDto(
            matchRequestId = "match-1",
            fromUserId = "user-1",
            toUserId = "user-2",
            status = "ACCEPTED",
            createdAt = "2026-08-01T00:00:00Z",
            respondedAt = "2026-08-02T00:00:00Z"
        )

        assertEquals(MatchRequestStatus.ACCEPTED, dto.toDomain().status)
    }
}
