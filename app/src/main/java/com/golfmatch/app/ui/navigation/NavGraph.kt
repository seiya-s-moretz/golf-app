package com.golfmatch.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.golfmatch.app.ui.component.GolfMatchBottomNavBar
import com.golfmatch.app.ui.component.bottomNavRoutes
import com.golfmatch.app.ui.container.BoardContainer
import com.golfmatch.app.ui.container.CreateBoardPostContainer
import com.golfmatch.app.ui.container.HomeContainer
import com.golfmatch.app.ui.container.MyPageContainer
import com.golfmatch.app.ui.container.RecommendContainer

/**
 * アプリ全体のナビゲーショングラフ（技術設計書 3章）。
 *
 * ホーム・おすすめユーザー・掲示板・マイページの4画面はフッターメニュー（[GolfMatchBottomNavBar]、
 * 技術設計書2-1章・3-3章）から遷移する。それ以外の画面（ラウンド詳細・新規投稿等）のCompose UI実装は
 * 次フェーズで行うため、引き続きRouteの配線確認用にプレースホルダー画面を割り当てている。
 * 実装時は各 composable ブロックを対応する `XxxContainer` に置き換える。
 */
@Composable
fun NavGraph(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                GolfMatchBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { outerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.route,
            modifier = Modifier.padding(outerPadding)
        ) {
            composable(Route.Home.route) { HomeContainer(navController = navController) }
            composable(Route.Recommend.route) { RecommendContainer(navController = navController) }
            composable(Route.Board.route) { BoardContainer(navController = navController) }
            composable(Route.MyPage.route) { MyPageContainer(navController = navController) }

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

            composable(Route.CreateBoardPost.route) { CreateBoardPostContainer(navController = navController) }

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
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text = "$name Screen (未実装)")
    }
}
