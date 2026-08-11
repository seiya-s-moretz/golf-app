package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.screen.round.CreateRoundScreen
import com.golfmatch.app.ui.viewmodel.CreateRoundViewModel

/**
 * ラウンド新規作成画面のNavController⇔CreateRoundViewModelの橋渡し（技術設計書6章 Containerパターン）。
 *
 * 作成成功時（[uiState.submitSuccess]）はホーム画面へ戻る。
 */
@Composable
fun CreateRoundContainer(
    navController: NavHostController,
    viewModel: CreateRoundViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            navController.popBackStack()
        }
    }

    CreateRoundScreen(
        uiState = uiState,
        onClubNameChange = { value -> viewModel.onClubNameChange(value) },
        onDateTimeChange = { value -> viewModel.onDateTimeChange(value) },
        onFeeChange = { value -> viewModel.onFeeChange(value) },
        onCapacityChange = { value -> viewModel.onCapacityChange(value) },
        onSubmitClick = { viewModel.submit() }
    )
}
