package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.AreaDto
import org.junit.Assert.assertEquals
import org.junit.Test

class AreaMapperTest {

    @Test
    fun `AreaDtoをtoDomainすると全フィールドが正しく変換される(ADR-0002)`() {
        val dto = AreaDto(
            areaId = "area-1",
            prefecture = "埼玉県",
            areaName = "さいたま市",
            displayOrder = 1,
            isActive = true,
            createdAt = "2026-01-01T00:00:00Z"
        )

        val domain = dto.toDomain()

        assertEquals("area-1", domain.areaId)
        assertEquals("埼玉県", domain.prefecture)
        assertEquals("さいたま市", domain.areaName)
        assertEquals(1, domain.displayOrder)
        assertEquals(true, domain.isActive)
    }

    @Test
    fun `isActive=falseのエリアも変換できる(将来エリアの先行登録、ADR-0002)`() {
        val dto = AreaDto(
            areaId = "area-2",
            prefecture = "千葉県",
            areaName = "未提供エリア",
            displayOrder = 99,
            isActive = false,
            createdAt = "2026-01-01T00:00:00Z"
        )

        assertEquals(false, dto.toDomain().isActive)
    }
}
