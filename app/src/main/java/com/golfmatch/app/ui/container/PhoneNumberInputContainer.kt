package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.auth.PhoneNumberInputScreen
import com.golfmatch.app.ui.viewmodel.PhoneNumberInputViewModel

/**
 * 電話番号入力画面のNavController⇔PhoneNumberInputViewModelの橋渡し（技術設計書6章 Containerパターン）。
 *
 * OTP送信成功時（[uiState.otpSent]）はOTP認証画面へ遷移する。
 */
@Composable
fun PhoneNumberInputContainer(
    navController: NavHostController,
    viewModel: PhoneNumberInputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.otpSent) {
        // 入力そのままではなく、実際にSMSを送った番号（E.164に正規化済み）を渡す。
        // 入力値を渡すと検証APIがE.164バリデーションで400になり認証を完了できない
        val phoneNumber = uiState.normalizedPhoneNumber
        if (uiState.otpSent && phoneNumber != null) {
            navController.navigate(Route.OtpVerification.createRoute(phoneNumber))
            viewModel.consumeOtpSent()
        }
    }

    PhoneNumberInputScreen(
        uiState = uiState,
        onPhoneNumberChange = { value -> viewModel.onPhoneNumberChange(value) },
        onSubmitClick = { viewModel.submit() }
    )
}
