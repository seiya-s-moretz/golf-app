package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.message.MessageListScreen
import com.golfmatch.app.ui.viewmodel.MessageListViewModel

/**
 * メッセージ一覧画面のNavController⇔MessageListViewModelの橋渡し（技術設計書6章 Containerパターン）。
 */
@Composable
fun MessageListContainer(
    navController: NavHostController,
    viewModel: MessageListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MessageListScreen(
        uiState = uiState,
        onConversationClick = { conversation ->
            navController.navigate(Route.MessageThread.createRoute(conversation.partner.userId))
        }
    )
}
