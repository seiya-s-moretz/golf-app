package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.screen.admin.ReportAdminDetailScreen
import com.golfmatch.app.ui.viewmodel.ReportAdminDetailViewModel

/**
 * 通報管理詳細画面（管理者向け）のNavController⇔ReportAdminDetailViewModelの橋渡し
 * （技術設計書6章 Containerパターン、ADR-0007）。
 */
@Composable
fun ReportAdminDetailContainer(
    navController: NavHostController,
    viewModel: ReportAdminDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ReportAdminDetailScreen(
        uiState = uiState,
        onStatusSelected = { viewModel.onStatusSelected(it) },
        onHandlingMemoChange = { viewModel.onHandlingMemoChange(it) },
        onSaveClick = { viewModel.save() }
    )
}
