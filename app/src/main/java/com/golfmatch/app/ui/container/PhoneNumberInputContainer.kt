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
        if (uiState.otpSent) {
            navController.navigate(Route.OtpVerification.createRoute(uiState.phoneNumber))
            viewModel.consumeOtpSent()
        }
    }

    PhoneNumberInputScreen(
        uiState = uiState,
        onPhoneNumberChange = { value -> viewModel.onPhoneNumberChange(value) },
        onSubmitClick = { viewModel.submit() }
    )
}
