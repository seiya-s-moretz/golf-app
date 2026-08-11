package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.screen.mypage.BlockedUsersScreen
import com.golfmatch.app.ui.viewmodel.BlockedUsersViewModel

/**
 * ブロック済みユーザー一覧画面のNavController⇔BlockedUsersViewModelの橋渡し（技術設計書6章 Containerパターン）。
 */
@Composable
fun BlockedUsersContainer(
    navController: NavHostController,
    viewModel: BlockedUsersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BlockedUsersScreen(
        uiState = uiState,
        onUnblockClick = { user -> viewModel.unblock(user.userId) }
    )
}
