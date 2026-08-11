package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.round.RoundDetailScreen
import com.golfmatch.app.ui.viewmodel.RoundDetailViewModel

/**
 * ラウンド詳細画面のNavController⇔RoundDetailViewModelの橋渡し（技術設計書6章 Containerパターン）。
 *
 * 主催者の場合は参加申請一覧画面（[Route.RoundJoinRequestList]）へ遷移する（ADR-0001）。
 */
@Composable
fun RoundDetailContainer(
    navController: NavHostController,
    viewModel: RoundDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    RoundDetailScreen(
        uiState = uiState,
        onApplyClick = { viewModel.applyJoin() },
        onViewJoinRequestsClick = {
            val eventId = uiState.roundEvent?.eventId ?: return@RoundDetailScreen
            navController.navigate(Route.RoundJoinRequestList.createRoute(eventId))
        }
    )
}
