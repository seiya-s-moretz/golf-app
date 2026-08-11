package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.auth.InitialProfileScreen
import com.golfmatch.app.ui.viewmodel.InitialProfileViewModel

/**
 * プロフィール初期登録画面のNavController⇔InitialProfileViewModelの橋渡し（技術設計書6章 Containerパターン）。
 *
 * 登録成功時（[uiState.submitSuccess]）はセッション確立済みのためホーム画面へ遷移し、
 * 認証フローのバックスタックは全てクリアする（戻るボタンで認証画面へ戻れないようにする）。
 */
@Composable
fun InitialProfileContainer(
    navController: NavHostController,
    viewModel: InitialProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            navController.navigate(Route.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    InitialProfileScreen(
        uiState = uiState,
        onNameChange = { value -> viewModel.onNameChange(value) },
        onGenderChange = { value -> viewModel.onGenderChange(value) },
        onAgeChange = { value -> viewModel.onAgeChange(value) },
        onAreaSelected = { area -> viewModel.onAreaSelected(area) },
        onAverageScoreChange = { value -> viewModel.onAverageScoreChange(value) },
        onPurposeSelected = { purpose -> viewModel.onPurposeSelected(purpose) },
        onIntroductionChange = { value -> viewModel.onIntroductionChange(value) },
        onSubmitClick = { viewModel.submit() }
    )
}
