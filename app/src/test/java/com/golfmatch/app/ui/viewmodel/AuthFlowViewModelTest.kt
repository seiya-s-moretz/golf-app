package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.golfmatch.app.data.auth.AuthSessionManager
import com.golfmatch.app.domain.model.AuthSession
import com.golfmatch.app.domain.model.PhoneOtpVerificationResult
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.model.RegistrationToken
import com.golfmatch.app.domain.repository.AuthRepository
import com.golfmatch.app.domain.usecase.GetAreasUseCase
import com.golfmatch.app.domain.usecase.RegisterUserUseCase
import com.golfmatch.app.domain.usecase.RequestPhoneOtpUseCase
import com.golfmatch.app.domain.usecase.VerifyPhoneOtpUseCase
import com.golfmatch.app.testutil.FakeAreaRepository
import com.golfmatch.app.testutil.FakeAuthRepository
import com.golfmatch.app.testutil.MainDispatcherRule
import com.golfmatch.app.testutil.TestFixtures
import com.golfmatch.app.ui.navigation.Route
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 本人確認（認証）フロー画面のViewModelテスト（技術設計書3-2章・7-2章、ADR-0003・ADR-0006）。
 *
 * `docs/test-plan.md` 4-3章で「認証フロー画面のViewModelテストが未整備」と記録されていた分の補完。
 * 電話番号入力 → OTP認証 → プロフィール初期登録 という3画面の分岐と、
 * 起動時の遷移先判定（[AppStartViewModel]）を対象とする。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthFlowViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ------------------------------------------------------------------
    // 電話番号入力画面
    // ------------------------------------------------------------------

    @Test
    fun `電話番号が空のままsubmitするとAPIを呼ばずエラーを表示する`() = runTest {
        val repo = FakeAuthRepository()
        val viewModel = PhoneNumberInputViewModel(RequestPhoneOtpUseCase(repo))

        viewModel.submit()

        assertNull(repo.lastOtpPhoneNumber)
        assertEquals("電話番号を入力してください", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.otpSent)
    }

    @Test
    fun `submitは前後の空白を除いた電話番号でOTPを要求しotpSentを立てる`() = runTest {
        val repo = FakeAuthRepository()
        val viewModel = PhoneNumberInputViewModel(RequestPhoneOtpUseCase(repo))

        viewModel.onPhoneNumberChange("  +819012345678  ")
        viewModel.submit()

        assertEquals("+819012345678", repo.lastOtpPhoneNumber)
        assertTrue(viewModel.uiState.value.otpSent)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `送信中に再度submitを呼んでもSMS送信は二重に行われない`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var otpCallCount = 0
        val repo = object : AuthRepository by FakeAuthRepository() {
            override suspend fun requestPhoneOtp(phoneNumber: String) {
                otpCallCount++
                gate.await()
            }
        }
        val viewModel = PhoneNumberInputViewModel(RequestPhoneOtpUseCase(repo))

        viewModel.onPhoneNumberChange("+819012345678")
        viewModel.submit()
        assertTrue(viewModel.uiState.value.isSubmitting)

        viewModel.submit()
        gate.complete(Unit)

        assertEquals(1, otpCallCount)
        assertTrue(viewModel.uiState.value.otpSent)
    }

    @Test
    fun `電話番号を編集し直すとotpSentとエラーがクリアされる`() = runTest {
        val viewModel = PhoneNumberInputViewModel(RequestPhoneOtpUseCase(FakeAuthRepository()))

        viewModel.onPhoneNumberChange("+819012345678")
        viewModel.submit()
        assertTrue(viewModel.uiState.value.otpSent)

        viewModel.onPhoneNumberChange("+819011112222")

        assertFalse(viewModel.uiState.value.otpSent)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ------------------------------------------------------------------
    // OTP認証画面（ADR-0006の新規/既存ユーザー分岐）
    // ------------------------------------------------------------------

    private fun otpViewModel(
        repo: AuthRepository,
        phoneNumber: String = "+819012345678"
    ) = OtpVerificationViewModel(
        SavedStateHandle(mapOf(Route.OtpVerification.ARG_PHONE_NUMBER to phoneNumber)),
        VerifyPhoneOtpUseCase(repo)
    )

    @Test
    fun `既存ユーザーの検証成功ではloginSuccessが立ちregistrationTokenは持たない`() = runTest {
        val repo = FakeAuthRepository(
            verifyResult = PhoneOtpVerificationResult.ExistingUser(
                AuthSession(accessToken = "access-token-1", userId = "user-1")
            )
        )
        val viewModel = otpViewModel(repo)

        viewModel.onOtpCodeChange("123456")
        viewModel.verify()

        val state = viewModel.uiState.value
        assertEquals("+819012345678" to "123456", repo.lastVerifyArgs)
        assertTrue(state.verifySuccess)
        assertTrue(state.loginSuccess)
        assertNull(state.registrationToken)
        assertFalse(state.isVerifying)
    }

    @Test
    fun `新規ユーザーの検証成功ではregistrationTokenが渡りloginSuccessは立たない`() = runTest {
        val repo = FakeAuthRepository(
            verifyResult = PhoneOtpVerificationResult.NewUser(RegistrationToken("reg-token-xyz"))
        )
        val viewModel = otpViewModel(repo)

        viewModel.onOtpCodeChange("123456")
        viewModel.verify()

        val state = viewModel.uiState.value
        assertTrue(state.verifySuccess)
        assertFalse(state.loginSuccess)
        assertEquals("reg-token-xyz", state.registrationToken)
    }

    @Test
    fun `確認コードが空のままverifyするとAPIを呼ばずエラーを表示する`() = runTest {
        val repo = FakeAuthRepository()
        val viewModel = otpViewModel(repo)

        viewModel.verify()

        assertNull(repo.lastVerifyArgs)
        assertEquals("確認コードを入力してください", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `検証中に再度verifyを呼んでもOTP検証は二重に行われない`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var verifyCallCount = 0
        val repo = object : AuthRepository by FakeAuthRepository() {
            override suspend fun verifyPhoneOtp(phoneNumber: String, otpCode: String): PhoneOtpVerificationResult {
                verifyCallCount++
                gate.await()
                return PhoneOtpVerificationResult.NewUser(RegistrationToken("reg-token-xyz"))
            }
        }
        val viewModel = otpViewModel(repo)

        viewModel.onOtpCodeChange("123456")
        viewModel.verify()
        assertTrue(viewModel.uiState.value.isVerifying)

        viewModel.verify()
        gate.complete(Unit)

        assertEquals(1, verifyCallCount)
    }

    @Test
    fun `検証失敗時はエラーメッセージを表示し成功フラグを立てない`() = runTest {
        val repo = object : AuthRepository by FakeAuthRepository() {
            override suspend fun verifyPhoneOtp(phoneNumber: String, otpCode: String): PhoneOtpVerificationResult =
                throw RuntimeException("確認コードが正しくありません")
        }
        val viewModel = otpViewModel(repo)

        viewModel.onOtpCodeChange("000000")
        viewModel.verify()

        val state = viewModel.uiState.value
        assertEquals("確認コードが正しくありません", state.errorMessage)
        assertFalse(state.verifySuccess)
        assertFalse(state.loginSuccess)
        assertFalse(state.isVerifying)
    }

    // ------------------------------------------------------------------
    // プロフィール初期登録画面
    // ------------------------------------------------------------------

    private fun initialProfileViewModel(
        authRepo: AuthRepository = FakeAuthRepository(),
        areaRepo: FakeAreaRepository = FakeAreaRepository(),
        registrationToken: String = "reg-token-xyz"
    ) = InitialProfileViewModel(
        SavedStateHandle(mapOf(Route.InitialProfile.ARG_REGISTRATION_TOKEN to registrationToken)),
        RegisterUserUseCase(authRepo),
        GetAreasUseCase(areaRepo)
    )

    @Test
    fun `初期表示でエリア一覧を取得する`() = runTest {
        val areas = listOf(TestFixtures.area(areaId = "area-1"), TestFixtures.area(areaId = "area-2"))
        val viewModel = initialProfileViewModel(areaRepo = FakeAreaRepository(areas = areas))

        assertEquals(2, viewModel.uiState.value.areaOptions.size)
        assertFalse(viewModel.uiState.value.isLoadingAreas)
    }

    @Test
    fun `必須項目が欠けているとAPIを呼ばず入力エラーを表示する`() = runTest {
        val repo = FakeAuthRepository()
        val viewModel = initialProfileViewModel(authRepo = repo)

        // エリア未選択・年齢未入力のまま送信
        viewModel.onNameChange("山田太郎")
        viewModel.submit()

        assertNull(repo.lastRegisterArgs)
        assertEquals("入力内容を確認してください", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `年齢とアベレージスコアが数値でない場合も入力エラーになる`() = runTest {
        val repo = FakeAuthRepository()
        val viewModel = initialProfileViewModel(authRepo = repo)

        viewModel.onNameChange("山田太郎")
        viewModel.onAreaSelected(TestFixtures.area(areaId = "area-1"))
        viewModel.onAgeChange("三十歳")
        viewModel.onAverageScoreChange("90")
        viewModel.submit()

        assertNull(repo.lastRegisterArgs)
        assertEquals("入力内容を確認してください", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `全項目が揃っていればregistrationTokenとともに登録APIを呼ぶ`() = runTest {
        val repo = FakeAuthRepository()
        val viewModel = initialProfileViewModel(authRepo = repo)

        viewModel.onNameChange("山田太郎")
        viewModel.onGenderChange("male")
        viewModel.onAgeChange("30")
        viewModel.onAreaSelected(TestFixtures.area(areaId = "area-1"))
        viewModel.onAverageScoreChange("90")
        viewModel.onPurposeSelected(Purpose.SERIOUS)
        viewModel.onIntroductionChange("よろしくお願いします")
        viewModel.submit()

        assertEquals(
            listOf<Any?>("reg-token-xyz", "山田太郎", "male", 30, "area-1", 90, Purpose.SERIOUS, "よろしくお願いします"),
            repo.lastRegisterArgs
        )
        assertTrue(viewModel.uiState.value.submitSuccess)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `登録中に再度submitを呼んでも登録APIは二重に呼ばれない`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var registerCallCount = 0
        val repo = object : AuthRepository by FakeAuthRepository() {
            override suspend fun registerUser(
                registrationToken: String,
                name: String,
                gender: String,
                age: Int,
                areaId: String,
                averageScore: Int,
                purpose: Purpose,
                introduction: String
            ): AuthSession {
                registerCallCount++
                gate.await()
                return AuthSession(accessToken = "access-token-1", userId = "user-1")
            }
        }
        val viewModel = initialProfileViewModel(authRepo = repo)

        viewModel.onNameChange("山田太郎")
        viewModel.onAreaSelected(TestFixtures.area(areaId = "area-1"))
        viewModel.onAgeChange("30")
        viewModel.onAverageScoreChange("90")
        viewModel.submit()
        assertTrue(viewModel.uiState.value.isSubmitting)

        viewModel.submit()
        gate.complete(Unit)

        assertEquals(1, registerCallCount)
        assertTrue(viewModel.uiState.value.submitSuccess)
    }

    // ------------------------------------------------------------------
    // 起動時の遷移先判定
    // ------------------------------------------------------------------

    @Test
    fun `セッションが無ければ起動時は電話番号入力画面へ`() {
        val sessionManager = AuthSessionManager()

        assertEquals(Route.PhoneNumberInput.route, AppStartViewModel(sessionManager).startDestination)
    }

    @Test
    fun `セッションがあれば起動時はホーム画面へ`() {
        val sessionManager = AuthSessionManager().apply { updateSession("access-token-1", "user-1") }

        assertEquals(Route.Home.route, AppStartViewModel(sessionManager).startDestination)
    }
}
