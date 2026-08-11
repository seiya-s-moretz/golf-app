package com.golfmatch.app.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.golfmatch.app.ui.navigation.Route

/**
 * フッターメニュー（技術設計書2-1章・3-3章）。
 *
 * 4タブ構成（ホーム／おすすめユーザー／掲示板／マイページ）。技術設計書3-3章のメッセージ一覧タブ
 * （5タブ化）は要確認事項として保留のため、本フェーズでは既存4画面のみを配線する。
 */
private data class BottomNavItem(val route: Route, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Route.Home, "ホーム", Icons.Filled.Home),
    BottomNavItem(Route.Recommend, "おすすめ", Icons.Filled.Groups),
    BottomNavItem(Route.Board, "掲示板", Icons.Filled.Forum),
    BottomNavItem(Route.MyPage, "マイページ", Icons.Filled.AccountCircle)
)

/** フッターメニューの対象ルート一覧（NavGraphがbottomBarの表示要否判定に利用） */
val bottomNavRoutes: Set<String> = bottomNavItems.map { it.route.route }.toSet()

@Composable
fun GolfMatchBottomNavBar(
    currentRoute: String?,
    onNavigate: (Route) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
