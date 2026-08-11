package com.golfmatch.app.ui.screen.recommend

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
 */
@Composable
fun RecommendScreen(
    uiState: RecommendUiState,
    onSendMatchRequest: (User) -> Unit = {}
) {
    Scaffold { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(innerPadding)
            uiState.errorMessage != null -> ErrorContent(innerPadding, uiState.errorMessage)
            uiState.users.isEmpty() -> EmptyContent(innerPadding)
            else -> UserList(innerPadding, uiState, onSendMatchRequest)
        }
    }
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(padding: PaddingValues, message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptyContent(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "現在おすすめできるユーザーはいません", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun UserList(
    padding: PaddingValues,
    uiState: RecommendUiState,
    onSendMatchRequest: (User) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(uiState.users, key = { it.userId }) { user ->
            UserCard(
                user = user,
                areaName = uiState.areaNames[user.areaId].orEmpty(),
                isRequested = user.userId in uiState.sentRequestUserIds,
                modifier = Modifier.padding(bottom = 12.dp),
                onSendMatchRequest = { onSendMatchRequest(user) }
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
