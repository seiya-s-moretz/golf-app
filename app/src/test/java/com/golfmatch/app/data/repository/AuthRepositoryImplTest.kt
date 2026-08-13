package com.golfmatch.app.data.repository

import com.golfmatch.app.data.api.ApiService
import com.golfmatch.app.data.auth.AuthSessionManager
import com.golfmatch.app.data.dto.AreaDto
import com.golfmatch.app.data.dto.AuthSessionResponseDto
import com.golfmatch.app.data.dto.RegisterUserRequestDto
import com.golfmatch.app.data.dto.RequestOtpRequestDto
import com.golfmatch.app.data.dto.UserDto
import com.golfmatch.app.data.dto.VerifyOtpRequestDto
import com.golfmatch.app.data.dto.VerifyOtpResponseDto
import com.golfmatch.app.data.repository.impl.AuthRepositoryImpl
import com.golfmatch.app.domain.model.PhoneOtpVerificationResult
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.testutil.FakeApiService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [AuthRepositoryImpl]のテスト（技術設計書6-1章、ADR-0003・ADR-0005・ADR-0006）。
 *
 * `docs/test-plan.md` 4-3章で「Repository実装層テストが未整備」と記録されていた分の補完。
 * 他のRepository実装が`ApiService`への薄い委譲のみであるのに対し、本実装だけは
 * **`is_new_user`による分岐と[AuthSessionManager]への副作用**を持つため、テスト対象とする価値が高い。
 * 「セッションが保存される／されない」の取り違えは、ログインできたのに自分が誰か分からない、
 * あるいは未認証のまま画面が進むといった形で表面化する。
 */
class AuthRepositoryImplTest {

    private fun userDto(userId: String = "user-1") = UserDto(
        userId = userId,
        name = "山田太郎",
        iconUrl = "",
        gender = "male",
        age = 30,
        area = AreaDto("area-1", "東京都", "東京23区", 1, true, "2026-01-01T00:00:00Z"),
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
    fun `requestPhoneOtpは電話番号をリクエストボディに詰めて送る`() = runTest {
        var sent: RequestOtpRequestDto? = null
        val api = object : ApiService by FakeApiService() {
            override suspend fun requestPhoneOtp(body: RequestOtpRequestDto) {
                sent = body
            }
        }
        val sessionManager = AuthSessionManager()

        AuthRepositoryImpl(api, sessionManager).requestPhoneOtp("+819012345678")

        assertEquals("+819012345678", sent?.phoneNumber)
        // OTP要求の時点ではまだ認証されていない
        assertNull(sessionManager.accessToken)
    }

    @Test
    fun `既存ユーザーの検証成功時はセッションが保存される(ADR-0006)`() = runTest {
        var sent: VerifyOtpRequestDto? = null
        val api = object : ApiService by FakeApiService() {
            override suspend fun verifyPhoneOtp(body: VerifyOtpRequestDto): VerifyOtpResponseDto {
                sent = body
                return VerifyOtpResponseDto(
                    isNewUser = false,
                    session = AuthSessionResponseDto(user = userDto("user-9"), accessToken = "access-token-9"),
                    registrationToken = null
                )
            }
        }
        val sessionManager = AuthSessionManager()

        val result = AuthRepositoryImpl(api, sessionManager).verifyPhoneOtp("+819012345678", "123456")

        assertEquals(VerifyOtpRequestDto("+819012345678", "123456"), sent)
        assertTrue(result is PhoneOtpVerificationResult.ExistingUser)
        assertEquals("access-token-9", sessionManager.accessToken)
        assertEquals("user-9", sessionManager.currentUserId)
    }

    @Test
    fun `新規ユーザーの検証成功時はセッションを保存しない(本登録がまだ完了していないため)`() = runTest {
        val api = object : ApiService by FakeApiService() {
            override suspend fun verifyPhoneOtp(body: VerifyOtpRequestDto): VerifyOtpResponseDto =
                VerifyOtpResponseDto(isNewUser = true, session = null, registrationToken = "reg-token-xyz")
        }
        val sessionManager = AuthSessionManager()

        val result = AuthRepositoryImpl(api, sessionManager).verifyPhoneOtp("+819012345678", "123456")

        assertTrue(result is PhoneOtpVerificationResult.NewUser)
        assertEquals("reg-token-xyz", (result as PhoneOtpVerificationResult.NewUser).registrationToken.value)
        assertNull(sessionManager.accessToken)
        assertNull(sessionManager.currentUserId)
    }

    @Test
    fun `既存ユーザーなのにsessionが欠落していたら契約違反として例外を投げセッションも汚さない(ADR-0005)`() {
        val api = object : ApiService by FakeApiService() {
            override suspend fun verifyPhoneOtp(body: VerifyOtpRequestDto): VerifyOtpResponseDto =
                VerifyOtpResponseDto(isNewUser = false, session = null, registrationToken = null)
        }
        val sessionManager = AuthSessionManager()

        // `runTest`はネストできないため、例外を検証するケースのみ`runBlocking`を使う
        assertThrows(IllegalStateException::class.java) {
            runBlocking { AuthRepositoryImpl(api, sessionManager).verifyPhoneOtp("+819012345678", "123456") }
        }
        assertNull(sessionManager.accessToken)
    }

    @Test
    fun `registerUserは入力をリクエストボディに詰めセッションを保存する`() = runTest {
        var sent: RegisterUserRequestDto? = null
        val api = object : ApiService by FakeApiService() {
            override suspend fun registerUser(body: RegisterUserRequestDto): AuthSessionResponseDto {
                sent = body
                return AuthSessionResponseDto(user = userDto("user-5"), accessToken = "access-token-5")
            }
        }
        val sessionManager = AuthSessionManager()

        val session = AuthRepositoryImpl(api, sessionManager).registerUser(
            registrationToken = "reg-token-xyz",
            name = "山田太郎",
            gender = "male",
            age = 30,
            areaId = "area-1",
            averageScore = 90,
            purpose = Purpose.SERIOUS,
            introduction = "よろしくお願いします"
        )

        assertEquals(
            RegisterUserRequestDto(
                registrationToken = "reg-token-xyz",
                name = "山田太郎",
                gender = "male",
                age = 30,
                areaId = "area-1",
                averageScore = 90,
                // Purposeはenum名の文字列としてサーバーへ送る
                purpose = "SERIOUS",
                introduction = "よろしくお願いします"
            ),
            sent
        )
        assertEquals("user-5", session.userId)
        assertEquals("access-token-5", sessionManager.accessToken)
        assertEquals("user-5", sessionManager.currentUserId)
    }

    @Test
    fun `登録レスポンスのuserが欠落していたら契約違反として例外を投げる(ADR-0005)`() {
        val api = object : ApiService by FakeApiService() {
            override suspend fun registerUser(body: RegisterUserRequestDto): AuthSessionResponseDto =
                AuthSessionResponseDto(user = null, accessToken = "access-token-5")
        }
        val sessionManager = AuthSessionManager()

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                AuthRepositoryImpl(api, sessionManager).registerUser(
                    registrationToken = "reg-token-xyz",
                    name = "山田太郎",
                    gender = "male",
                    age = 30,
                    areaId = "area-1",
                    averageScore = 90,
                    purpose = Purpose.CASUAL,
                    introduction = ""
                )
            }
        }
        // userIdが空文字列でセッションが張られてしまう事故（4-1章で解消済みのバグ）が再発していないこと
        assertNull(sessionManager.accessToken)
        assertNull(sessionManager.currentUserId)
    }
}
