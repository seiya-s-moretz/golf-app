package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.screen.recommend.RecommendScreen
import com.golfmatch.app.ui.viewmodel.RecommendViewModel

/**
 * おすすめユーザー画面のNavController⇔RecommendViewModelの橋渡し（技術設計書6章 Containerパターン）。
 */
@Composable
fun RecommendContainer(
    navController: NavHostController,
    viewModel: RecommendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    RecommendScreen(
        uiState = uiState,
        onSendMatchRequest = { user -> viewModel.sendMatchRequest(user) }
    )
}
