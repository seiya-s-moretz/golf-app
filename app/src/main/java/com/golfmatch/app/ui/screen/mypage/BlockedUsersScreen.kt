package com.golfmatch.app.ui.screen.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.BlockedUsersUiState
import kotlinx.datetime.Instant

/**
 * ブロック済みユーザー一覧画面（技術設計書3-2章・7-2章）。
 *
 * 各ユーザーに「ブロック解除」ボタンを表示する（`DELETE /users/{id}/block`、技術設計書6-3章）。
 */
@Composable
fun BlockedUsersScreen(
    uiState: BlockedUsersUiState,
    onUnblockClick: (User) -> Unit = {}
) {
    Scaffold { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(innerPadding)
            uiState.errorMessage != null && uiState.blockedUsers.isEmpty() ->
                ErrorContent(innerPadding, uiState.errorMessage)
            uiState.blockedUsers.isEmpty() -> EmptyContent(innerPadding)
            else -> BlockedUserList(innerPadding, uiState, onUnblockClick)
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
        Text(text = "ブロックしているユーザーはいません", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BlockedUserList(
    padding: PaddingValues,
    uiState: BlockedUsersUiState,
    onUnblockClick: (User) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(uiState.blockedUsers, key = { it.userId }) { user ->
                BlockedUserCard(
                    user = user,
                    isProcessing = uiState.processingUserId == user.userId,
                    modifier = Modifier.padding(bottom = 12.dp),
                    onUnblockClick = { onUnblockClick(user) }
                )
            }
        }
    }
}

@Composable
private fun BlockedUserCard(
    user: User,
    isProcessing: Boolean,
    modifier: Modifier = Modifier,
    onUnblockClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.Person, contentDescription = null)
            }
            Column(
                modifier = Modifier.padding(start = 12.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = user.name, style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(onClick = onUnblockClick, enabled = !isProcessing) {
                Text("ブロック解除")
            }
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
        introduction = "",
        phoneNumber = "+819012345678",
        phoneVerified = true,
        phoneVerifiedAt = Instant.parse("2026-08-01T00:00:00Z"),
        status = AccountStatus.ACTIVE,
        createdAt = Instant.parse("2026-08-01T00:00:00Z")
    )
)

@Preview(showBackground = true, name = "一覧表示")
@Composable
private fun BlockedUsersScreenPreview() {
    GolfMatchTheme {
        BlockedUsersScreen(uiState = BlockedUsersUiState(blockedUsers = previewUsers()))
    }
}

@Preview(showBackground = true, name = "ローディング")
@Composable
private fun BlockedUsersScreenLoadingPreview() {
    GolfMatchTheme {
        BlockedUsersScreen(uiState = BlockedUsersUiState(isLoading = true))
    }
}

@Preview(showBackground = true, name = "空状態")
@Composable
private fun BlockedUsersScreenEmptyPreview() {
    GolfMatchTheme {
        BlockedUsersScreen(uiState = BlockedUsersUiState(blockedUsers = emptyList()))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun BlockedUsersScreenErrorPreview() {
    GolfMatchTheme {
        BlockedUsersScreen(uiState = BlockedUsersUiState(errorMessage = "ブロック済みユーザー一覧の取得に失敗しました"))
    }
}
