package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.screen.recommend.MatchRequestListScreen
import com.golfmatch.app.ui.viewmodel.MatchRequestListViewModel

/**
 * 受信マッチング申請一覧画面のNavController⇔MatchRequestListViewModelの橋渡し
 * （技術設計書6章 Containerパターン）。
 */
@Composable
fun MatchRequestListContainer(
    navController: NavHostController,
    viewModel: MatchRequestListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MatchRequestListScreen(
        uiState = uiState,
        onApproveClick = { request -> viewModel.respond(request.matchRequestId, approve = true) },
        onRejectClick = { request -> viewModel.respond(request.matchRequestId, approve = false) }
    )
}
