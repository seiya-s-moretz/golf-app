package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.screen.message.MessageThreadScreen
import com.golfmatch.app.ui.viewmodel.MessageThreadViewModel

/**
 * メッセージスレッド画面のNavController⇔MessageThreadViewModelの橋渡し（技術設計書6章 Containerパターン）。
 */
@Composable
fun MessageThreadContainer(
    navController: NavHostController,
    viewModel: MessageThreadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MessageThreadScreen(
        uiState = uiState,
        onInputTextChange = { text -> viewModel.onInputTextChange(text) },
        onSendClick = { viewModel.send() }
    )
}
