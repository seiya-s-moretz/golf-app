package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.RoundEventDto
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundEventMapperTest {

    @Test
    fun `RoundEventDtoをtoDomainすると全フィールドが正しく変換される`() {
        val dto = RoundEventDto(
            eventId = "event-1",
            clubName = "サンプルゴルフ倶楽部",
            datetime = "2026-09-01T00:00:00Z",
            fee = 8000,
            capacity = 4,
            current = 2,
            createdBy = "user-1",
            createdAt = "2026-08-01T00:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals("event-1", domain.eventId)
        assertEquals("サンプルゴルフ倶楽部", domain.clubName)
        assertEquals(Instant.parse("2026-09-01T00:00:00Z"), domain.datetime)
        assertEquals(8000, domain.fee)
        assertEquals(4, domain.capacity)
        // current: 参加申請が「承認」された時点でのみ加算される値（ADR-0001）。
        // マッパーは単純な値の受け渡しのみを行い、承認ロジック自体はサーバー側の責務。
        assertEquals(2, domain.current)
        assertEquals("user-1", domain.createdBy)
        assertEquals(Instant.parse("2026-08-01T00:00:00Z"), domain.createdAt)
    }
}
