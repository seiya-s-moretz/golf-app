package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.ReportDto
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.model.ReportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReportMapperTest {

    @Test
    fun `reason_textがnull(OTHER以外)でも変換できる`() {
        val dto = ReportDto(
            reportId = "report-1",
            reporterUserId = "user-1",
            targetType = "USER",
            targetId = "user-2",
            reasonCategory = "SPAM",
            reasonText = null,
            status = "PENDING",
            createdAt = "2026-08-01T00:00:00Z",
            handledByUserId = null,
            handledAt = null,
            handlingMemo = null
        )

        val domain = dto.toDomain()

        assertEquals(ReportTargetType.USER, domain.targetType)
        assertEquals(ReportReasonCategory.SPAM, domain.reasonCategory)
        assertNull(domain.reasonText)
        assertEquals(ReportStatus.PENDING, domain.status)
        assertNull(domain.handledAt)
    }

    @Test
    fun `DATING_SOLICITATION理由・BOARD_POST対象も正しく変換される(PRD 恋愛目的利用禁止の具体化)`() {
        val dto = ReportDto(
            reportId = "report-2",
            reporterUserId = "user-1",
            targetType = "BOARD_POST",
            targetId = "post-1",
            reasonCategory = "DATING_SOLICITATION",
            reasonText = null,
            status = "PENDING",
            createdAt = "2026-08-01T00:00:00Z",
            handledByUserId = null,
            handledAt = null,
            handlingMemo = null
        )

        val domain = dto.toDomain()

        assertEquals(ReportTargetType.BOARD_POST, domain.targetType)
        assertEquals(ReportReasonCategory.DATING_SOLICITATION, domain.reasonCategory)
    }

    @Test
    fun `OTHER理由の場合はreasonTextが値を保持する`() {
        val dto = ReportDto(
            reportId = "report-3",
            reporterUserId = "user-1",
            targetType = "USER",
            targetId = "user-2",
            reasonCategory = "OTHER",
            reasonText = "自由記述の理由",
            status = "REVIEWING",
            createdAt = "2026-08-01T00:00:00Z",
            handledByUserId = null,
            handledAt = "2026-08-02T00:00:00Z",
            handlingMemo = null
        )

        val domain = dto.toDomain()

        assertEquals("自由記述の理由", domain.reasonText)
        assertEquals(ReportStatus.REVIEWING, domain.status)
    }
}
