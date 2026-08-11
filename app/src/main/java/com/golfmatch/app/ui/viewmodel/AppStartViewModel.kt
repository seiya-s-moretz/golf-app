package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.golfmatch.app.data.auth.AuthSessionManager
import com.golfmatch.app.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * アプリ起動時の遷移先判定（技術設計書3-2章、ADR-0003）。
 *
 * [AuthSessionManager]にセッション（アクセストークン）が無ければ本人確認フローの入口
 * （電話番号入力画面）を、あればホーム画面（既存4タブ）を起動時の最初の画面とする。
 * 判定はプロセス起動時点（本ViewModel生成時点）の一度きりで、以降のログイン/ログアウトによる
 * 画面遷移は各Containerのナビゲーション処理（[com.golfmatch.app.ui.container.OtpVerificationContainer]等）
 * が担う。
 */
@HiltViewModel
class AppStartViewModel @Inject constructor(
    authSessionManager: AuthSessionManager
) : ViewModel() {
    val startDestination: String =
        if (authSessionManager.accessToken != null) Route.Home.route else Route.PhoneNumberInput.route
}
