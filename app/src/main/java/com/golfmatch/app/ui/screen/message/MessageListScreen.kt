package com.golfmatch.app.ui.screen.message

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.domain.model.AccountStatus
import com.golfmatch.app.domain.model.Conversation
import com.golfmatch.app.domain.model.Message
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.model.User
import com.golfmatch.app.ui.component.ConversationListItem
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.MessageListUiState
import kotlinx.datetime.Instant

/**
 * メッセージ一覧画面（技術設計書3-2章・7-2章）。
 *
 * `Connection`が存在するユーザーペア単位の会話一覧を新着順（`updatedAt`降順）で表示する（6-7章）。
 */
@Composable
fun MessageListScreen(
    uiState: MessageListUiState,
    onConversationClick: (Conversation) -> Unit = {}
) {
    Scaffold { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(innerPadding)
            uiState.errorMessage != null && uiState.conversations.isEmpty() ->
                ErrorContent(innerPadding, uiState.errorMessage)
            uiState.conversations.isEmpty() -> EmptyContent(innerPadding)
            else -> ConversationList(innerPadding, uiState, onConversationClick)
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
        Text(text = "メッセージのやり取りはまだありません", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ConversationList(
    padding: PaddingValues,
    uiState: MessageListUiState,
    onConversationClick: (Conversation) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(uiState.conversations, key = { it.partner.userId }) { conversation ->
                ConversationListItem(
                    conversation = conversation,
                    onClick = { onConversationClick(conversation) }
                )
                HorizontalDivider()
            }
        }
    }
}

private fun previewConversations() = listOf(
    Conversation(
        partner = User(
            userId = "user-2",
            name = "田中太郎",
            iconUrl = "",
            gender = "male",
            age = 28,
            areaId = "area-1",
            averageScore = 90,
            purpose = Purpose.CASUAL,
            introduction = "",
            phoneNumber = "+819011112222",
            phoneVerified = true,
            phoneVerifiedAt = Instant.parse("2026-08-01T00:00:00Z"),
            status = AccountStatus.ACTIVE,
            createdAt = Instant.parse("2026-08-01T00:00:00Z")
        ),
        lastMessage = Message(
            messageId = "msg-1",
            userAId = "user-1",
            userBId = "user-2",
            senderId = "user-2",
            content = "今度のラウンドよろしくお願いします！",
            createdAt = Instant.parse("2026-08-11T10:00:00Z"),
            readAt = null
        ),
        unreadCount = 1,
        updatedAt = Instant.parse("2026-08-11T10:00:00Z")
    )
)

@Preview(showBackground = true, name = "一覧表示")
@Composable
private fun MessageListScreenPreview() {
    GolfMatchTheme {
        MessageListScreen(uiState = MessageListUiState(conversations = previewConversations()))
    }
}

@Preview(showBackground = true, name = "ローディング")
@Composable
private fun MessageListScreenLoadingPreview() {
    GolfMatchTheme {
        MessageListScreen(uiState = MessageListUiState(isLoading = true))
    }
}

@Preview(showBackground = true, name = "空状態")
@Composable
private fun MessageListScreenEmptyPreview() {
    GolfMatchTheme {
        MessageListScreen(uiState = MessageListUiState(conversations = emptyList()))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun MessageListScreenErrorPreview() {
    GolfMatchTheme {
        MessageListScreen(uiState = MessageListUiState(errorMessage = "会話一覧の取得に失敗しました"))
    }
}
