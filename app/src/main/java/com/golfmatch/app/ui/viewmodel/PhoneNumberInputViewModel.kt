package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.golfmatch.app.domain.usecase.RequestPhoneOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 電話番号入力画面のUiState（技術設計書7-2章 `PhoneNumberInputUiState`） */
data class PhoneNumberInputUiState(
    val phoneNumber: String = "",
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
        val phoneNumber = _uiState.value.phoneNumber.trim()
        if (phoneNumber.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "電話番号を入力してください")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching { requestPhoneOtpUseCase(phoneNumber) }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, otpSent = true)
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
