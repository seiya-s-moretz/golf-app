package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.recommend.RecommendScreen
import com.golfmatch.app.ui.viewmodel.RecommendViewModel

/**
 * おすすめユーザー画面のNavController⇔RecommendViewModelの橋渡し（技術設計書6章 Containerパターン）。
 *
 * 受信マッチング申請一覧画面（[Route.MatchRequestList]、技術設計書3-2章）へ遷移する導線に加え、
 * ユーザーカードの「…」メニューから通報画面（[Route.Report]、技術設計書7-3章）への導線を提供する。
 * ブロックはブロック確認ダイアログ（`UserCard`内）→[RecommendViewModel.blockUser]で完結するため
 * 画面遷移は発生しない。
 */
@Composable
fun RecommendContainer(
    navController: NavHostController,
    viewModel: RecommendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    RecommendScreen(
        uiState = uiState,
        onSendMatchRequest = { user -> viewModel.sendMatchRequest(user) },
        onViewMatchRequestsClick = { navController.navigate(Route.MatchRequestList.route) },
        onReportUser = { user ->
            navController.navigate(Route.Report.createRoute(ReportTargetType.USER.name, user.userId))
        },
        onBlockUser = { user -> viewModel.blockUser(user) },
        onLoadMore = { viewModel.loadMore() }
    )
}
