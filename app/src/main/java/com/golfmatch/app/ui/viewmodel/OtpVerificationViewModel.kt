package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.PhoneOtpVerificationResult
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
 * （ADR-0006）の結果を画面に伝えるために追加した
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
 * OTP認証画面のViewModel（技術設計書3-2章・7-2章、ADR-0003、ADR-0006）。
 *
 * 新規/既存ユーザーの判定は[verifyPhoneOtpUseCase]の呼び出し1回で確定する（ADR-0006）。
 * 戻り値が[PhoneOtpVerificationResult.ExistingUser]ならセッション開始済みのためホーム画面へ、
 * [PhoneOtpVerificationResult.NewUser]なら`registrationToken`をセットしプロフィール初期登録画面へ遷移する。
 */
@HiltViewModel
class OtpVerificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val verifyPhoneOtpUseCase: VerifyPhoneOtpUseCase
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
        // 検証中および検証成功後の二重タップを弾く。OTPは検証成功時に消費されるため、2回目の呼び出しは
        // 必ず失敗する。成功直後は画面遷移が完了するまでボタンが操作可能な状態で残るため、
        // `isVerifying`だけでは塞げない
        if (state.isVerifying || state.verifySuccess) return
        if (state.otpCode.isBlank()) {
            _uiState.value = state.copy(errorMessage = "確認コードを入力してください")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifying = true, errorMessage = null)

            runCatching { verifyPhoneOtpUseCase(state.phoneNumber, state.otpCode) }
                .onSuccess { result ->
                    _uiState.value = when (result) {
                        is PhoneOtpVerificationResult.ExistingUser -> _uiState.value.copy(
                            isVerifying = false,
                            verifySuccess = true,
                            loginSuccess = true
                        )
                        is PhoneOtpVerificationResult.NewUser -> _uiState.value.copy(
                            isVerifying = false,
                            verifySuccess = true,
                            registrationToken = result.registrationToken.value
                        )
                    }
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
