package com.golfmatch.app.ui.screen.recommend

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.domain.model.AccountStatus
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.model.User
import com.golfmatch.app.ui.component.UserCard
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.RecommendUiState
import kotlinx.datetime.Instant

/**
 * おすすめユーザー画面（技術設計書3-1章、`D:\勉強\golf\基本設計書.md` 3-2章）。
 *
 * 自分宛のマッチング申請一覧画面（[Route.MatchRequestList][com.golfmatch.app.ui.navigation.Route.MatchRequestList]、
 * 技術設計書3-2章）への導線をこの画面から提供する。
 */
@Composable
fun RecommendScreen(
    uiState: RecommendUiState,
    onSendMatchRequest: (User) -> Unit = {},
    onViewMatchRequestsClick: () -> Unit = {},
    onReportUser: (User) -> Unit = {},
    onBlockUser: (User) -> Unit = {}
) {
    Scaffold { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedButton(
                onClick = onViewMatchRequestsClick,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("受信したマッチング申請を見る")
            }
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> LoadingContent()
                    uiState.errorMessage != null -> ErrorContent(uiState.errorMessage)
                    uiState.users.isEmpty() -> EmptyContent()
                    else -> UserList(uiState, onSendMatchRequest, onReportUser, onBlockUser)
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "現在おすすめできるユーザーはいません", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun UserList(
    uiState: RecommendUiState,
    onSendMatchRequest: (User) -> Unit,
    onReportUser: (User) -> Unit,
    onBlockUser: (User) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(uiState.users, key = { it.userId }) { user ->
            UserCard(
                user = user,
                areaName = uiState.areaNames[user.areaId].orEmpty(),
                isRequested = user.userId in uiState.sentRequestUserIds,
                modifier = Modifier.padding(bottom = 12.dp),
                onSendMatchRequest = { onSendMatchRequest(user) },
                onReportClick = { onReportUser(user) },
                onBlockUser = { onBlockUser(user) }
            )
        }
    }
}

private fun previewUsers() = listOf(
    User(
        userId = "user-1",
        name = "鈴木一郎",
        iconUrl = "",
        gender = "male",
        age = 32,
        areaId = "area-1",
        averageScore = 95,
        purpose = Purpose.CASUAL,
        introduction = "初心者ですが楽しくラウンドしたいです",
        phoneNumber = "+819012345678",
        phoneVerified = true,
        phoneVerifiedAt = Instant.parse("2026-08-01T00:00:00Z"),
        status = AccountStatus.ACTIVE,
        createdAt = Instant.parse("2026-08-01T00:00:00Z")
    ),
    User(
        userId = "user-2",
        name = "佐藤花子",
        iconUrl = "",
        gender = "female",
        age = 28,
        areaId = "area-2",
        averageScore = 88,
        purpose = Purpose.SERIOUS,
        introduction = "本気で上達したいです",
        phoneNumber = "+819012345679",
        phoneVerified = true,
        phoneVerifiedAt = Instant.parse("2026-08-01T00:00:00Z"),
        status = AccountStatus.ACTIVE,
        createdAt = Instant.parse("2026-08-01T00:00:00Z")
    )
)

@Preview(showBackground = true, name = "一覧表示")
@Composable
private fun RecommendScreenPreview() {
    GolfMatchTheme {
        RecommendScreen(
            uiState = RecommendUiState(
                users = previewUsers(),
                areaNames = mapOf("area-1" to "さいたま市", "area-2" to "川口市")
            )
        )
    }
}

@Preview(showBackground = true, name = "ローディング")
@Composable
private fun RecommendScreenLoadingPreview() {
    GolfMatchTheme {
        RecommendScreen(uiState = RecommendUiState(isLoading = true))
    }
}

@Preview(showBackground = true, name = "空状態")
@Composable
private fun RecommendScreenEmptyPreview() {
    GolfMatchTheme {
        RecommendScreen(uiState = RecommendUiState(users = emptyList()))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun RecommendScreenErrorPreview() {
    GolfMatchTheme {
        RecommendScreen(uiState = RecommendUiState(errorMessage = "おすすめユーザー一覧の取得に失敗しました"))
    }
}
