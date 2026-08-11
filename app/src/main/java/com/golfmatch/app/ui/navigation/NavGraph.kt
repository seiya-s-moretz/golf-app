package com.golfmatch.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.golfmatch.app.ui.container.HomeContainer

/**
 * アプリ全体のナビゲーショングラフ（技術設計書 3章）。
 *
 * ホーム画面（`HomeContainer`）以外の各画面のCompose UI実装（Screen/Container/ViewModel）は
 * 次フェーズで行うため、引き続きRouteの配線確認用にプレースホルダー画面を割り当てている。
 * 実装時は各 composable ブロックを対応する `XxxContainer` に置き換える。
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Route.Home.route) {
        composable(Route.Home.route) { HomeContainer(navController = navController) }
        composable(Route.Recommend.route) { PlaceholderScreen("Recommend") }
        composable(Route.Board.route) { PlaceholderScreen("Board") }
        composable(Route.MyPage.route) { PlaceholderScreen("MyPage") }

        composable(Route.CreateRound.route) { PlaceholderScreen("CreateRound") }
        composable(
            route = Route.RoundDetail.route,
            arguments = listOf(navArgument(Route.RoundDetail.ARG_EVENT_ID) { type = NavType.StringType })
        ) { PlaceholderScreen("RoundDetail") }
        composable(
            route = Route.RoundJoinRequestList.route,
            arguments = listOf(navArgument(Route.RoundJoinRequestList.ARG_EVENT_ID) { type = NavType.StringType })
        ) { PlaceholderScreen("RoundJoinRequestList") }

        composable(Route.MatchRequestList.route) { PlaceholderScreen("MatchRequestList") }

        composable(Route.CreateBoardPost.route) { PlaceholderScreen("CreateBoardPost") }

        composable(Route.BlockedUsers.route) { PlaceholderScreen("BlockedUsers") }

        composable(Route.MessageList.route) { PlaceholderScreen("MessageList") }
        composable(
            route = Route.MessageThread.route,
            arguments = listOf(navArgument(Route.MessageThread.ARG_PARTNER_ID) { type = NavType.StringType })
        ) { PlaceholderScreen("MessageThread") }

        composable(
            route = Route.Report.route,
            arguments = listOf(
                navArgument(Route.Report.ARG_TARGET_TYPE) { type = NavType.StringType },
                navArgument(Route.Report.ARG_TARGET_ID) { type = NavType.StringType }
            )
        ) { PlaceholderScreen("Report") }

        composable(Route.PhoneNumberInput.route) { PlaceholderScreen("PhoneNumberInput") }
        composable(
            route = Route.OtpVerification.route,
            arguments = listOf(navArgument(Route.OtpVerification.ARG_PHONE_NUMBER) { type = NavType.StringType })
        ) { PlaceholderScreen("OtpVerification") }
        composable(
            route = Route.InitialProfile.route,
            arguments = listOf(navArgument(Route.InitialProfile.ARG_REGISTRATION_TOKEN) { type = NavType.StringType })
        ) { PlaceholderScreen("InitialProfile") }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text = "$name Screen (未実装)")
    }
}
