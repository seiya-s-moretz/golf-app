package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.RoundJoinRequestDto
import com.golfmatch.app.domain.model.RoundJoinRequestStatus
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoundJoinRequestMapperTest {

    @Test
    fun `PENDING状態ではrespondedAtがnullのまま変換される`() {
        val dto = RoundJoinRequestDto(
            joinRequestId = "join-1",
            eventId = "event-1",
            userId = "user-2",
            status = "PENDING",
            createdAt = "2026-08-01T00:00:00Z",
            respondedAt = null
        )

        val domain = dto.toDomain()

        assertEquals(RoundJoinRequestStatus.PENDING, domain.status)
        assertNull(domain.respondedAt)
    }

    @Test
    fun `APPROVED状態ではrespondedAtが変換される`() {
        val dto = RoundJoinRequestDto(
            joinRequestId = "join-1",
            eventId = "event-1",
            userId = "user-2",
            status = "APPROVED",
            createdAt = "2026-08-01T00:00:00Z",
            respondedAt = "2026-08-02T00:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals(RoundJoinRequestStatus.APPROVED, domain.status)
        assertEquals(Instant.parse("2026-08-02T00:00:00Z"), domain.respondedAt)
    }

    @Test
    fun `REJECTED状態も正しく変換される`() {
        val dto = RoundJoinRequestDto(
            joinRequestId = "join-1",
            eventId = "event-1",
            userId = "user-2",
            status = "REJECTED",
            createdAt = "2026-08-01T00:00:00Z",
            respondedAt = "2026-08-02T00:00:00Z"
        )

        assertEquals(RoundJoinRequestStatus.REJECTED, dto.toDomain().status)
    }
}
