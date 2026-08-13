package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.message.MessageThreadScreen
import com.golfmatch.app.ui.viewmodel.MessageThreadViewModel

/**
 * メッセージスレッド画面のNavController⇔MessageThreadViewModelの橋渡し（技術設計書6章 Containerパターン）。
 *
 * 会話相手の通報・ブロック導線を持つ（技術設計書7-3章）。ブロック後はこの会話自体が
 * サーバーから返らなくなる（一覧から除外・履歴取得も403）ため、画面を閉じてメッセージ一覧へ戻る。
 */
@Composable
fun MessageThreadContainer(
    navController: NavHostController,
    viewModel: MessageThreadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.blockSuccess) {
        if (uiState.blockSuccess) {
            navController.popBackStack()
        }
    }

    MessageThreadScreen(
        uiState = uiState,
        onInputTextChange = { text -> viewModel.onInputTextChange(text) },
        onSendClick = { viewModel.send() },
        onLoadOlder = { viewModel.loadOlder() },
        onBlockUser = { viewModel.blockUser() },
        onReportUser = {
            navController.navigate(Route.Report.createRoute(ReportTargetType.USER.name, uiState.partnerId))
        }
    )
}
