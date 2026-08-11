package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.usecase.LoginUseCase
import com.golfmatch.app.domain.usecase.VerifyPhoneOtpUseCase
import com.golfmatch.app.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * OTP認証画面のUiState（技術設計書7-2章 `OtpVerificationUiState`）。
 *
 * [loginSuccess] は設計書のUiState定義には無いが、検証成功後の新規/既存ユーザー分岐
 * （[OtpVerificationViewModel]のKDoc参照）の結果を画面に伝えるために追加した
 * （既存パターン[MyPageUiState]等と同様、実装判断としての最小限の追加）。
 */
data class OtpVerificationUiState(
    val phoneNumber: String = "",
    val otpCode: String = "",
    val isVerifying: Boolean = false,
    val verifySuccess: Boolean = false,
    val registrationToken: String? = null,
    val loginSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * OTP認証画面のViewModel（技術設計書3-2章・7-2章、ADR-0003）。
 *
 * ## 要確認事項：検証成功後の新規/既存ユーザー分岐
 * 技術設計書6-1章には `POST /auth/phone/verify`（OTP検証・`registration_token`発行）と
 * `POST /auth/login`（電話番号+OTPでの再ログイン）が別APIとして定義されているが、
 * 「OTP入力画面に来た時点で新規登録か再ログインかをクライアントがどう判別するか」の記述が無い。
 * `verify`のレスポンスにも新規/既存を示すフィールドは定義されていない（`VerifyOtpResponseDto`は
 * `registration_token`のみ）。
 *
 * 本実装では次の暫定方針を採った: まず[loginUseCase]（`POST /auth/login`）を試行し、
 * 成功すれば「既存アカウントが存在した」とみなしてそのままセッションを保存しホーム画面へ遷移する。
 * 失敗した場合は「アカウント未作成」とみなし[verifyPhoneOtpUseCase]（`POST /auth/phone/verify`）で
 * `registration_token`を取得し、プロフィール初期登録画面へ遷移する。
 *
 * この方針には既知の課題がある: ログイン失敗の原因が「アカウントが存在しない」なのか
 * 「OTP不一致・期限切れ等の他のエラー」なのかをエラー内容から区別する契約が設計書に無いため、
 * 後者の場合も誤って新規登録フローに倒れてしまう可能性がある。また同一OTPコードで2つのAPIを
 * 呼び出す（OTPが1回限り有効な実装だった場合、2回目の呼び出しが失敗する）前提の齟齬もあり得る。
 * ArchitectAgentに、(1) `POST /auth/phone/verify`のレスポンスに「既存ユーザーか否か」を示す
 * フィールドを追加するか、(2) 電話番号入力の時点で新規/既存を判定するAPIを設けるか、
 * の設計判断を確認することを推奨する。
 */
@HiltViewModel
class OtpVerificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val verifyPhoneOtpUseCase: VerifyPhoneOtpUseCase,
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OtpVerificationUiState(
            phoneNumber = checkNotNull(savedStateHandle.get<String>(Route.OtpVerification.ARG_PHONE_NUMBER)) {
                "OtpVerificationScreen requires phoneNumber argument"
            }
        )
    )
    val uiState: StateFlow<OtpVerificationUiState> = _uiState.asStateFlow()

    fun onOtpCodeChange(value: String) {
        _uiState.value = _uiState.value.copy(otpCode = value, errorMessage = null)
    }

    fun verify() {
        val state = _uiState.value
        if (state.otpCode.isBlank()) {
            _uiState.value = state.copy(errorMessage = "確認コードを入力してください")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifying = true, errorMessage = null)

            val loginResult = runCatching { loginUseCase(state.phoneNumber, state.otpCode) }
            if (loginResult.isSuccess) {
                // 既存ユーザー: そのままセッション開始済み（LoginUseCase内のRepositoryがAuthSessionManagerを更新する）
                _uiState.value = _uiState.value.copy(isVerifying = false, verifySuccess = true, loginSuccess = true)
                return@launch
            }

            runCatching { verifyPhoneOtpUseCase(state.phoneNumber, state.otpCode) }
                .onSuccess { token ->
                    _uiState.value = _uiState.value.copy(
                        isVerifying = false,
                        verifySuccess = true,
                        registrationToken = token.value
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isVerifying = false,
                        errorMessage = error.message ?: "確認コードの検証に失敗しました"
                    )
                }
        }
    }
}
