package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.board.BoardScreen
import com.golfmatch.app.ui.viewmodel.BoardViewModel

/**
 * 掲示板画面のNavController⇔BoardViewModelの橋渡し（技術設計書6章 Containerパターン）。
 *
 * 投稿カードの「…」メニューから通報画面（[Route.Report]、技術設計書7-3章）への導線を提供する。
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
        },
        onReportPost = { post ->
            navController.navigate(Route.Report.createRoute(ReportTargetType.BOARD_POST.name, post.postId))
        }
    )
}
