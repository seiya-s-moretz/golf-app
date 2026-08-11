package com.golfmatch.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.golfmatch.app.ui.component.GolfMatchBottomNavBar
import com.golfmatch.app.ui.component.bottomNavRoutes
import com.golfmatch.app.ui.container.BlockedUsersContainer
import com.golfmatch.app.ui.container.BoardContainer
import com.golfmatch.app.ui.container.CreateBoardPostContainer
import com.golfmatch.app.ui.container.CreateRoundContainer
import com.golfmatch.app.ui.container.HomeContainer
import com.golfmatch.app.ui.container.InitialProfileContainer
import com.golfmatch.app.ui.container.MatchRequestListContainer
import com.golfmatch.app.ui.container.MessageListContainer
import com.golfmatch.app.ui.container.MessageThreadContainer
import com.golfmatch.app.ui.container.MyPageContainer
import com.golfmatch.app.ui.container.OtpVerificationContainer
import com.golfmatch.app.ui.container.PhoneNumberInputContainer
import com.golfmatch.app.ui.container.RecommendContainer
import com.golfmatch.app.ui.container.ReportAdminDetailContainer
import com.golfmatch.app.ui.container.ReportAdminListContainer
import com.golfmatch.app.ui.container.ReportContainer
import com.golfmatch.app.ui.container.RoundDetailContainer
import com.golfmatch.app.ui.container.RoundJoinRequestListContainer
import com.golfmatch.app.ui.viewmodel.AppStartViewModel

/**
 * アプリ全体のナビゲーショングラフ（技術設計書 3章）。
 *
 * ホーム・おすすめユーザー・掲示板・メッセージ一覧・マイページの5画面はフッターメニュー
 * （[GolfMatchBottomNavBar]、技術設計書2-1章・3-3章）から遷移する。通報管理画面（管理者向け、3-4章、ADR-0007）は
 * フッターメニューには含めず、マイページの「通報管理」メニュー項目（`User.is_admin=true`の場合のみ表示）から遷移する。
 *
 * 起動時の最初の画面は[AppStartViewModel]が判定する（技術設計書3-2章、ADR-0003）。
 * [com.golfmatch.app.data.auth.AuthSessionManager]にセッションが無ければ本人確認フロー
 * （電話番号入力画面）、あればホーム画面から開始する。
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    appStartViewModel: AppStartViewModel = hiltViewModel()
) {
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
            startDestination = appStartViewModel.startDestination,
            modifier = Modifier.padding(outerPadding)
        ) {
            composable(Route.Home.route) { HomeContainer(navController = navController) }
            composable(Route.Recommend.route) { RecommendContainer(navController = navController) }
            composable(Route.Board.route) { BoardContainer(navController = navController) }
            composable(Route.MyPage.route) { MyPageContainer(navController = navController) }

            composable(Route.CreateRound.route) { CreateRoundContainer(navController = navController) }
            composable(
                route = Route.RoundDetail.route,
                arguments = listOf(navArgument(Route.RoundDetail.ARG_EVENT_ID) { type = NavType.StringType })
            ) { RoundDetailContainer(navController = navController) }
            composable(
                route = Route.RoundJoinRequestList.route,
                arguments = listOf(navArgument(Route.RoundJoinRequestList.ARG_EVENT_ID) { type = NavType.StringType })
            ) { RoundJoinRequestListContainer(navController = navController) }

            composable(Route.MatchRequestList.route) { MatchRequestListContainer(navController = navController) }

            composable(Route.CreateBoardPost.route) { CreateBoardPostContainer(navController = navController) }

            composable(Route.BlockedUsers.route) { BlockedUsersContainer(navController = navController) }

            composable(Route.ReportAdminList.route) { ReportAdminListContainer(navController = navController) }
            composable(
                route = Route.ReportAdminDetail.route,
                arguments = listOf(navArgument(Route.ReportAdminDetail.ARG_REPORT_ID) { type = NavType.StringType })
            ) { ReportAdminDetailContainer(navController = navController) }

            composable(Route.MessageList.route) { MessageListContainer(navController = navController) }
            composable(
                route = Route.MessageThread.route,
                arguments = listOf(navArgument(Route.MessageThread.ARG_PARTNER_ID) { type = NavType.StringType })
            ) { MessageThreadContainer(navController = navController) }

            composable(
                route = Route.Report.route,
                arguments = listOf(
                    navArgument(Route.Report.ARG_TARGET_TYPE) { type = NavType.StringType },
                    navArgument(Route.Report.ARG_TARGET_ID) { type = NavType.StringType }
                )
            ) { ReportContainer(navController = navController) }

            composable(Route.PhoneNumberInput.route) { PhoneNumberInputContainer(navController = navController) }
            composable(
                route = Route.OtpVerification.route,
                arguments = listOf(navArgument(Route.OtpVerification.ARG_PHONE_NUMBER) { type = NavType.StringType })
            ) { OtpVerificationContainer(navController = navController) }
            composable(
                route = Route.InitialProfile.route,
                arguments = listOf(navArgument(Route.InitialProfile.ARG_REGISTRATION_TOKEN) { type = NavType.StringType })
            ) { InitialProfileContainer(navController = navController) }
        }
    }
}
