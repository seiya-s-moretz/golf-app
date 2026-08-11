package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.board.BoardScreen
import com.golfmatch.app.ui.viewmodel.BoardViewModel

/**
 * 掲示板画面のNavController⇔BoardViewModelの橋渡し（技術設計書6章 Containerパターン）。
 */
@Composable
fun BoardContainer(
    navController: NavHostController,
    viewModel: BoardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BoardScreen(
        uiState = uiState,
        onCreatePostClick = {
            navController.navigate(Route.CreateBoardPost.route)
        }
    )
}
