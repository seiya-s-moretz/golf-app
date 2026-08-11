package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.screen.round.RoundJoinRequestListScreen
import com.golfmatch.app.ui.viewmodel.RoundJoinRequestListViewModel

/**
 * ラウンド参加申請一覧画面（主催者向け）のNavController⇔RoundJoinRequestListViewModelの橋渡し
 * （技術設計書6章 Containerパターン）。
 */
@Composable
fun RoundJoinRequestListContainer(
    navController: NavHostController,
    viewModel: RoundJoinRequestListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    RoundJoinRequestListScreen(
        uiState = uiState,
        onApproveClick = { request -> viewModel.respond(request.joinRequestId, approve = true) },
        onRejectClick = { request -> viewModel.respond(request.joinRequestId, approve = false) }
    )
}
