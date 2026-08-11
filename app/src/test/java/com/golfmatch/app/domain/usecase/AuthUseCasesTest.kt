package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.PhoneOtpVerificationResult
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.testutil.FakeAuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 本人確認・認証関連UseCaseのテスト（技術設計書6-1章、ADR-0003）。
 *
 * OTPの実際の送信・検証（ブルートフォース対策の試行回数管理、有効期限管理等）はサーバー側の責務であり
 * クライアントには実装がない。ここではUseCase→Repositoryへの委譲を確認する。
 */
class AuthUseCasesTest {

    @Test
    fun `RequestPhoneOtpUseCaseはphoneNumberをそのまま渡す`() = runBlocking {
        val repo = FakeAuthRepository()
        val useCase = RequestPhoneOtpUseCase(repo)

        useCase("+819012345678")

        assertEquals("+819012345678", repo.lastOtpPhoneNumber)
    }

    @Test
    fun `VerifyPhoneOtpUseCaseはphoneNumberとotpCodeをそのまま渡しPhoneOtpVerificationResultを返す(ADR-0006)`() = runBlocking {
        val repo = FakeAuthRepository()
        val useCase = VerifyPhoneOtpUseCase(repo)

        val result = useCase("+819012345678", "123456")

        assertEquals("+819012345678" to "123456", repo.lastVerifyArgs)
        assertTrue(result is PhoneOtpVerificationResult.NewUser)
        assertEquals("reg-token-1", (result as PhoneOtpVerificationResult.NewUser).registrationToken.value)
    }

    @Test
    fun `RegisterUserUseCaseは全パラメータをそのままRepositoryへ渡す(技術設計書6-1章 POST users)`() = runBlocking {
        val repo = FakeAuthRepository()
        val useCase = RegisterUserUseCase(repo)

        val result = useCase(
            registrationToken = "reg-token-1",
            name = "山田太郎",
            gender = "male",
            age = 30,
            areaId = "area-1",
            averageScore = 90,
            purpose = Purpose.CASUAL,
            introduction = "よろしくお願いします"
        )

        assertEquals(
            listOf("reg-token-1", "山田太郎", "male", 30, "area-1", 90, Purpose.CASUAL, "よろしくお願いします"),
            repo.lastRegisterArgs
        )
        assertEquals("user-1", result.userId)
        assertEquals("access-token-1", result.accessToken)
    }
}
