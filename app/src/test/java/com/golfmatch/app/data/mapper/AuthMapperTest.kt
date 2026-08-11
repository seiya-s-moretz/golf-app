package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.AreaDto
import com.golfmatch.app.data.dto.AuthSessionResponseDto
import com.golfmatch.app.data.dto.UserDto
import com.golfmatch.app.data.dto.VerifyOtpResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthMapperTest {

    @Test
    fun `VerifyOtpResponseDtoはregistrationTokenを保持したまま変換される(技術設計書6-1章)`() {
        val dto = VerifyOtpResponseDto(registrationToken = "reg-token-xyz")
        assertEquals("reg-token-xyz", dto.toDomain().value)
    }

    @Test
    fun `新規登録レスポンス(userを含む)はuserIdが正しく変換される(POST users)`() {
        val userDto = UserDto(
            userId = "user-1",
            name = "山田太郎",
            iconUrl = "",
            gender = "male",
            age = 30,
            area = AreaDto("area-1", "埼玉県", "さいたま市", 1, true, "2026-01-01T00:00:00Z"),
            averageScore = 90,
            purpose = "CASUAL",
            introduction = "",
            phoneNumber = "+819012345678",
            phoneVerified = true,
            phoneVerifiedAt = "2026-01-01T00:00:00Z",
            status = "ACTIVE",
            createdAt = "2026-01-01T00:00:00Z"
        )
        val dto = AuthSessionResponseDto(user = userDto, accessToken = "access-token-abc")

        val domain = dto.toDomain()

        assertEquals("user-1", domain.userId)
        assertEquals("access-token-abc", domain.accessToken)
    }

    /**
     * ADR-0005対応: `user` は `POST /auth/login` のレスポンスでも契約上必須（技術設計書6-1章）。
     * `user` が欠落している場合はサーバー側の契約違反であり、`userId` を空文字列へ
     * サイレントにフォールバックさせず、明示的に例外を送出することを確認する。
     */
    @Test
    fun `userを含まないレスポンスをtoDomainすると例外がスローされる(ADR-0005)`() {
        val dto = AuthSessionResponseDto(user = null, accessToken = "access-token-abc")

        assertThrows(IllegalStateException::class.java) {
            dto.toDomain()
        }
    }
}
