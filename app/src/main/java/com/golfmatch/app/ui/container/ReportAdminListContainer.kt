package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.admin.ReportAdminListScreen
import com.golfmatch.app.ui.viewmodel.ReportAdminListViewModel

/**
 * 通報管理一覧画面（管理者向け）のNavController⇔ReportAdminListViewModelの橋渡し
 * （技術設計書6章 Containerパターン、ADR-0007）。
 */
@Composable
fun ReportAdminListContainer(
    navController: NavHostController,
    viewModel: ReportAdminListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ReportAdminListScreen(
        uiState = uiState,
        onStatusFilterSelected = { viewModel.onStatusFilterSelected(it) },
        onReportClick = { summary ->
            navController.navigate(Route.ReportAdminDetail.createRoute(summary.report.reportId))
        }
    )
}
