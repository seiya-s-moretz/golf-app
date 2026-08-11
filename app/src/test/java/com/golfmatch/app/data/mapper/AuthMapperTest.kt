package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.AreaDto
import com.golfmatch.app.data.dto.AuthSessionResponseDto
import com.golfmatch.app.data.dto.UserDto
import com.golfmatch.app.data.dto.VerifyOtpResponseDto
import com.golfmatch.app.domain.model.PhoneOtpVerificationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthMapperTest {

    private fun userDto(userId: String = "user-1") = UserDto(
        userId = userId,
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

    @Test
    fun `is_new_userがtrueの場合NewUserとしてregistrationTokenを保持したまま変換される(ADR-0006)`() {
        val dto = VerifyOtpResponseDto(isNewUser = true, session = null, registrationToken = "reg-token-xyz")

        val result = dto.toDomain()

        assertTrue(result is PhoneOtpVerificationResult.NewUser)
        assertEquals("reg-token-xyz", (result as PhoneOtpVerificationResult.NewUser).registrationToken.value)
    }

    @Test
    fun `is_new_userがtrueでregistration_tokenがnullの場合は例外がスローされる(ADR-0006)`() {
        val dto = VerifyOtpResponseDto(isNewUser = true, session = null, registrationToken = null)

        assertThrows(IllegalStateException::class.java) {
            dto.toDomain()
        }
    }

    @Test
    fun `is_new_userがfalseの場合ExistingUserとしてsessionが変換される(ADR-0006)`() {
        val sessionDto = AuthSessionResponseDto(user = userDto(), accessToken = "access-token-abc")
        val dto = VerifyOtpResponseDto(isNewUser = false, session = sessionDto, registrationToken = null)

        val result = dto.toDomain()

        assertTrue(result is PhoneOtpVerificationResult.ExistingUser)
        val existingUser = result as PhoneOtpVerificationResult.ExistingUser
        assertEquals("user-1", existingUser.session.userId)
        assertEquals("access-token-abc", existingUser.session.accessToken)
    }

    @Test
    fun `is_new_userがfalseでsessionがnullの場合は例外がスローされる(ADR-0006)`() {
        val dto = VerifyOtpResponseDto(isNewUser = false, session = null, registrationToken = null)

        assertThrows(IllegalStateException::class.java) {
            dto.toDomain()
        }
    }

    @Test
    fun `新規登録レスポンス(userを含む)はuserIdが正しく変換される(POST users)`() {
        val dto = AuthSessionResponseDto(user = userDto(), accessToken = "access-token-abc")

        val domain = dto.toDomain()

        assertEquals("user-1", domain.userId)
        assertEquals("access-token-abc", domain.accessToken)
    }

    /**
     * ADR-0005対応: `user` は `POST /auth/phone/verify`（既存ユーザー分岐）のレスポンスでも
     * 契約上必須（技術設計書6-1章、ADR-0006）。
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
