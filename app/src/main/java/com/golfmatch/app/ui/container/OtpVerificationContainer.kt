package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.auth.OtpVerificationScreen
import com.golfmatch.app.ui.viewmodel.OtpVerificationViewModel

/**
 * OTP認証画面のNavController⇔OtpVerificationViewModelの橋渡し（技術設計書6章 Containerパターン）。
 *
 * 検証成功時（[uiState.verifySuccess]）の遷移先は新規/既存ユーザーで分岐する
 * （[OtpVerificationViewModel]のKDoc参照、要確認事項）。
 * - 既存ユーザー（[uiState.loginSuccess]）: セッション確立済みのためホーム画面へ遷移し、認証フローの
 *   バックスタックは全てクリアする（戻るボタンで認証画面へ戻れないようにする）
 * - 新規ユーザー（[uiState.registrationToken]あり）: プロフィール初期登録画面へ遷移する
 */
@Composable
fun OtpVerificationContainer(
    navController: NavHostController,
    viewModel: OtpVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.verifySuccess) {
        if (!uiState.verifySuccess) return@LaunchedEffect

        if (uiState.loginSuccess) {
            navController.navigate(Route.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            val token = uiState.registrationToken
            if (token != null) {
                // OTP認証画面はバックスタックから外す。検証済みのOTPは消費されており戻っても再利用できず、
                // 残したままだと戻る操作で`verifySuccess=true`のまま再コンポーズされ、即座に前進してしまう
                // （戻れなくなる）。戻り先は電話番号入力画面とし、番号を入れ直せるようにする
                navController.navigate(Route.InitialProfile.createRoute(token)) {
                    popUpTo(Route.PhoneNumberInput.route) { inclusive = false }
                }
            }
        }
    }

    OtpVerificationScreen(
        uiState = uiState,
        onOtpCodeChange = { value -> viewModel.onOtpCodeChange(value) },
        onVerifyClick = { viewModel.verify() }
    )
}
