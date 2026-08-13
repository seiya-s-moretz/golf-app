package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.message.MessageListScreen
import com.golfmatch.app.ui.viewmodel.MessageListViewModel

/**
 * メッセージ一覧画面のNavController⇔MessageListViewModelの橋渡し（技術設計書6章 Containerパターン）。
 *
 * トーク画面から戻ったときに最新メッセージ・未読件数が反映されるよう、画面復帰時に再読み込みする
 * （ViewModelの初期化時のみの取得では、バックスタックに残る本画面は更新されない）。
 */
@Composable
fun MessageListContainer(
    navController: NavHostController,
    viewModel: MessageListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }

    MessageListScreen(
        uiState = uiState,
        onConversationClick = { conversation ->
            navController.navigate(Route.MessageThread.createRoute(conversation.partner.userId))
        }
    )
}
