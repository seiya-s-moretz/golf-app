package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.mypage.MyPageScreen
import com.golfmatch.app.ui.viewmodel.MyPageViewModel

/**
 * マイページ画面のNavController⇔MyPageViewModelの橋渡し（技術設計書6章 Containerパターン）。
 *
 * ブロック済みユーザー一覧画面（[Route.BlockedUsers]、技術設計書3-2章）、
 * 通報管理一覧画面（[Route.ReportAdminList]、`User.is_admin=true`の場合のみ表示、技術設計書3-4章・ADR-0007）
 * への導線を提供する。
 */
@Composable
fun MyPageContainer(
    navController: NavHostController,
    viewModel: MyPageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MyPageScreen(
        uiState = uiState,
        onNameChange = { viewModel.onNameChange(it) },
        onGenderChange = { viewModel.onGenderChange(it) },
        onAgeChange = { viewModel.onAgeChange(it) },
        onAreaSelected = { viewModel.onAreaSelected(it) },
        onAverageScoreChange = { viewModel.onAverageScoreChange(it) },
        onPurposeSelected = { viewModel.onPurposeSelected(it) },
        onIntroductionChange = { viewModel.onIntroductionChange(it) },
        onSaveClick = { viewModel.save() },
        onBlockedUsersClick = { navController.navigate(Route.BlockedUsers.route) },
        onReportAdminClick = { navController.navigate(Route.ReportAdminList.route) }
    )
}
