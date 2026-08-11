package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.screen.report.ReportScreen
import com.golfmatch.app.ui.viewmodel.ReportViewModel

/**
 * 通報画面のNavController⇔ReportViewModelの橋渡し（技術設計書6章 Containerパターン）。
 *
 * 送信成功時（[uiState.submitSuccess]）は遷移元（掲示板投稿・ユーザー詳細画面等）へ戻る。
 */
@Composable
fun ReportContainer(
    navController: NavHostController,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            navController.popBackStack()
        }
    }

    ReportScreen(
        uiState = uiState,
        onReasonCategorySelected = { category -> viewModel.onReasonCategorySelected(category) },
        onReasonTextChange = { text -> viewModel.onReasonTextChange(text) },
        onSubmitClick = { viewModel.submit() }
    )
}
