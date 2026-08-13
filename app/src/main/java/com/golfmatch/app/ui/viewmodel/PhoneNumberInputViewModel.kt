package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.model.PhoneNumberNormalizer
import com.golfmatch.app.domain.usecase.RequestPhoneOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 電話番号入力画面のUiState（技術設計書7-2章 `PhoneNumberInputUiState`）。
 *
 * [phoneNumber]はユーザーが入力したままの文字列、[normalizedPhoneNumber]はそれをE.164へ正規化した
 * 「実際にサーバーへ送った番号」。OTP認証画面へは後者を渡す必要がある（前者を渡すと、SMSは届いたのに
 * 検証APIがE.164バリデーションで400になり、認証を完了できなくなる）。
 */
data class PhoneNumberInputUiState(
    val phoneNumber: String = "",
    val normalizedPhoneNumber: String? = null,
    val isSubmitting: Boolean = false,
    val otpSent: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PhoneNumberInputViewModel @Inject constructor(
    private val requestPhoneOtpUseCase: RequestPhoneOtpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneNumberInputUiState())
    val uiState: StateFlow<PhoneNumberInputUiState> = _uiState.asStateFlow()

    fun onPhoneNumberChange(value: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = value, errorMessage = null, otpSent = false)
    }

    fun submit() {
        // 送信中の二重タップを弾く（他画面のガードパターンと統一）。SMS送信は実費が発生するため、
        // 重複リクエストは避けたい（技術設計書12-5章）
        if (_uiState.value.isSubmitting) return
        if (_uiState.value.phoneNumber.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "電話番号を入力してください")
            return
        }
        // サーバーはE.164形式しか受け付けないため、国内表記（先頭0・ハイフン区切り）をここで変換する。
        // 変換できない入力はAPIを呼ぶ前に弾き、「HTTP 400」のような生のエラーを見せない
        val phoneNumber = PhoneNumberNormalizer.normalizeOrNull(_uiState.value.phoneNumber)
        if (phoneNumber == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "電話番号の形式が正しくありません（例: 09012345678）"
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching { requestPhoneOtpUseCase(phoneNumber) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        otpSent = true,
                        normalizedPhoneNumber = phoneNumber
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "SMSの送信に失敗しました"
                    )
                }
        }
    }

    /** OTP認証画面への遷移後、画面復帰時に再送信フラグを消費済みにする */
    fun consumeOtpSent() {
        _uiState.value = _uiState.value.copy(otpSent = false)
    }
}
