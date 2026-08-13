package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.home.HomeScreen
import com.golfmatch.app.ui.viewmodel.HomeViewModel

/**
 * ホーム画面のNavController⇔HomeViewModelの橋渡し（技術設計書6章 Containerパターン）。
 *
 * 一覧の取得はViewModelの初期化時のみ行われるが、ホーム画面はバックスタックに残り続けるため、
 * ラウンド募集作成から戻ってきても再初期化されない（＝作った募集が一覧に出ない）。
 * 画面が再び前面に戻ったタイミングで再読み込みする。
 */
@Composable
fun HomeContainer(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LifecycleResumeEffect(Unit) {
        viewModel.loadRoundEvents()
        onPauseOrDispose { }
    }

    HomeScreen(
        uiState = uiState,
        onRoundEventClick = { roundEvent ->
            navController.navigate(Route.RoundDetail.createRoute(roundEvent.eventId))
        },
        onCreateRoundClick = {
            navController.navigate(Route.CreateRound.route)
        }
    )
}
