package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.AreaDto
import com.golfmatch.app.data.dto.UserDto
import com.golfmatch.app.domain.model.AccountStatus
import com.golfmatch.app.domain.model.Purpose
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class UserMapperTest {

    private fun areaDto(areaId: String = "area-1") = AreaDto(
        areaId = areaId,
        prefecture = "埼玉県",
        areaName = "さいたま市",
        displayOrder = 1,
        isActive = true,
        createdAt = "2026-01-01T00:00:00Z"
    )

    private fun userDto(
        purpose: String = "CASUAL",
        phoneVerified: Boolean = true,
        phoneVerifiedAt: String? = "2026-02-01T00:00:00Z",
        status: String = "ACTIVE"
    ) = UserDto(
        userId = "user-1",
        name = "山田太郎",
        iconUrl = "https://example.com/icon.png",
        gender = "male",
        age = 30,
        area = areaDto(),
        averageScore = 90,
        purpose = purpose,
        introduction = "よろしくお願いします",
        phoneNumber = "+819012345678",
        phoneVerified = phoneVerified,
        phoneVerifiedAt = phoneVerifiedAt,
        status = status,
        createdAt = "2026-01-15T12:00:00Z"
    )

    @Test
    fun `UserDtoをtoDomainすると全フィールドが正しく変換される`() {
        val domain = userDto().toDomain()

        assertEquals("user-1", domain.userId)
        assertEquals("山田太郎", domain.name)
        assertEquals("https://example.com/icon.png", domain.iconUrl)
        assertEquals("male", domain.gender)
        assertEquals(30, domain.age)
        assertEquals("area-1", domain.areaId) // area(AreaDtoオブジェクト)からarea_idを取り出す
        assertEquals(90, domain.averageScore)
        assertEquals(Purpose.CASUAL, domain.purpose)
        assertEquals("よろしくお願いします", domain.introduction)
        assertEquals("+819012345678", domain.phoneNumber)
        assertEquals(true, domain.phoneVerified)
        assertEquals(Instant.parse("2026-02-01T00:00:00Z"), domain.phoneVerifiedAt)
        assertEquals(AccountStatus.ACTIVE, domain.status)
        assertEquals(Instant.parse("2026-01-15T12:00:00Z"), domain.createdAt)
    }

    @Test
    fun `phoneVerifiedAtがnullの場合はnullのまま変換される(未確認ユーザー)`() {
        val domain = userDto(phoneVerified = false, phoneVerifiedAt = null).toDomain()

        assertEquals(false, domain.phoneVerified)
        assertNull(domain.phoneVerifiedAt)
    }

    @Test
    fun `purposeが日本語ラベルで来ても解決できる`() {
        val domain = userDto(purpose = "ガチ").toDomain()
        assertEquals(Purpose.SERIOUS, domain.purpose)
    }

    @Test
    fun `statusがSUSPENDEDの場合も正しく変換される`() {
        val domain = userDto(status = "SUSPENDED").toDomain()
        assertEquals(AccountStatus.SUSPENDED, domain.status)
    }

    @Test
    fun `未知のstatus値の場合は例外を投げる(サーバーとの契約違反を検出できる)`() {
        assertThrows(IllegalArgumentException::class.java) {
            userDto(status = "UNKNOWN").toDomain()
        }
    }
}
