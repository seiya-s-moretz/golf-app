package com.golfmatch.app.ui.screen.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.domain.model.Message
import com.golfmatch.app.ui.component.MessageBubble
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.MessageThreadUiState
import kotlinx.datetime.Instant

/**
 * メッセージスレッド画面（技術設計書3-2章・7-2章）。
 *
 * シンプルなチャットUI（メッセージ一覧＋下部にテキスト入力＋送信ボタン）。既読/未読の細かい制御
 * （`read_at`更新API呼び出し）は行わず表示のみに留める（技術設計書6-7章・[com.golfmatch.app.ui.viewmodel.MessageThreadViewModel]参照）。
 * 1:1会話のため、送信者IDが[MessageThreadUiState.partnerId]と一致しないメッセージは自分の発言として表示する。
 *
 * [MessageThreadUiState.messages]は古い順（末尾が最新）で渡される。上端に近づくと[onLoadOlder]で
 * 過去メッセージを遡って読み込み（技術設計書6-7章の`before`/`limit`）、最新メッセージが増えたときは
 * 末尾までスクロールする。
 */
@Composable
fun MessageThreadScreen(
    uiState: MessageThreadUiState,
    onInputTextChange: (String) -> Unit = {},
    onSendClick: () -> Unit = {},
    onLoadOlder: () -> Unit = {}
) {
    Scaffold(
        bottomBar = {
            MessageInputBar(
                inputText = uiState.inputText,
                isSending = uiState.isSending,
                onInputTextChange = onInputTextChange,
                onSendClick = onSendClick
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(innerPadding)
            uiState.errorMessage != null && uiState.messages.isEmpty() ->
                ErrorContent(innerPadding, uiState.errorMessage)
            else -> MessageThreadContent(innerPadding, uiState, onLoadOlder)
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

/** スレッド上端から何件手前で過去メッセージの読み込みを開始するか */
private const val LOAD_OLDER_THRESHOLD = 3

@Composable
private fun MessageThreadContent(
    padding: PaddingValues,
    uiState: MessageThreadUiState,
    onLoadOlder: () -> Unit
) {
    val listState = rememberLazyListState()
    val shouldLoadOlder by remember(uiState.messages.size, uiState.hasOlder) {
        derivedStateOf { uiState.hasOlder && listState.firstVisibleItemIndex <= LOAD_OLDER_THRESHOLD }
    }
    LaunchedEffect(shouldLoadOlder) {
        if (shouldLoadOlder) onLoadOlder()
    }

    // 最新メッセージが増えたとき（初回表示・送信直後）だけ末尾へスクロールする。
    // 過去メッセージの読み込みでは末尾のIDが変わらないため、閲覧位置を奪わない。
    val latestMessageId = uiState.messages.lastOrNull()?.messageId
    LaunchedEffect(latestMessageId) {
        if (latestMessageId != null) listState.scrollToItem(uiState.messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Text(
            text = uiState.partnerName.ifEmpty { "トーク" },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        HorizontalDivider()

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (uiState.messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "メッセージはまだありません", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            // 過去メッセージの読み込み状態はリスト外（上端）に出す。LazyColumnの要素にすると
            // 表示の有無でインデックスがずれ、末尾へのスクロール位置計算が壊れるため。
            when {
                uiState.isLoadingOlder -> LoadOlderIndicator()
                uiState.hasOlder && uiState.errorMessage != null -> LoadOlderRetryButton(onClick = onLoadOlder)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages, key = { it.messageId }) { message ->
                    MessageBubble(message = message, isMine = message.senderId != uiState.partnerId)
                }
            }
        }
    }
}

@Composable
private fun LoadOlderIndicator() {
    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadOlderRetryButton(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(4.dp), contentAlignment = Alignment.Center) {
        TextButton(onClick = onClick) { Text("過去のメッセージを再読み込み") }
    }
}

@Composable
private fun MessageInputBar(
    inputText: String,
    isSending: Boolean,
    onInputTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("メッセージを入力") },
            enabled = !isSending
        )
        IconButton(onClick = onSendClick, enabled = !isSending && inputText.isNotBlank()) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "送信")
        }
    }
}

private fun previewMessages() = listOf(
    Message(
        messageId = "msg-1",
        userAId = "user-1",
        userBId = "user-2",
        senderId = "user-2",
        content = "今度のラウンドよろしくお願いします！",
        createdAt = Instant.parse("2026-08-11T10:00:00Z"),
        readAt = null
    ),
    Message(
        messageId = "msg-2",
        userAId = "user-1",
        userBId = "user-2",
        senderId = "user-1",
        content = "こちらこそよろしくお願いします！",
        createdAt = Instant.parse("2026-08-11T10:05:00Z"),
        readAt = null
    )
)

@Preview(showBackground = true, name = "スレッド表示")
@Composable
private fun MessageThreadScreenPreview() {
    GolfMatchTheme {
        MessageThreadScreen(
            uiState = MessageThreadUiState(
                partnerId = "user-2",
                partnerName = "田中太郎",
                messages = previewMessages()
            )
        )
    }
}

@Preview(showBackground = true, name = "ローディング")
@Composable
private fun MessageThreadScreenLoadingPreview() {
    GolfMatchTheme {
        MessageThreadScreen(uiState = MessageThreadUiState(isLoading = true))
    }
}

@Preview(showBackground = true, name = "空状態")
@Composable
private fun MessageThreadScreenEmptyPreview() {
    GolfMatchTheme {
        MessageThreadScreen(uiState = MessageThreadUiState(partnerId = "user-2", partnerName = "田中太郎"))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun MessageThreadScreenErrorPreview() {
    GolfMatchTheme {
        MessageThreadScreen(
            uiState = MessageThreadUiState(errorMessage = "メッセージ履歴の取得に失敗しました")
        )
    }
}
