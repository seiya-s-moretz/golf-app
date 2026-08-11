package com.golfmatch.app.ui.container

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.golfmatch.app.ui.navigation.Route
import com.golfmatch.app.ui.screen.home.HomeScreen
import com.golfmatch.app.ui.viewmodel.HomeViewModel

/**
 * ホーム画面のNavController⇔HomeViewModelの橋渡し（技術設計書6章 Containerパターン）。
 */
@Composable
fun HomeContainer(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
