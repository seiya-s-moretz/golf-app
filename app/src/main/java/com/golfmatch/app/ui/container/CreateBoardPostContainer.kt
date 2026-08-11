package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.screen.board.CreateBoardPostScreen
import com.golfmatch.app.ui.viewmodel.CreateBoardPostViewModel

/**
 * 掲示板新規投稿画面のNavController⇔CreateBoardPostViewModelの橋渡し（技術設計書6章 Containerパターン）。
 *
 * 投稿成功時（[uiState.submitSuccess]）は掲示板一覧画面へ戻る。
 */
@Composable
fun CreateBoardPostContainer(
    navController: NavHostController,
    viewModel: CreateBoardPostViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            navController.popBackStack()
        }
    }

    CreateBoardPostScreen(
        uiState = uiState,
        onContentChange = { content -> viewModel.onContentChange(content) },
        onSubmitClick = { viewModel.submit() }
    )
}
