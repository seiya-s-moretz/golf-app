package com.golfmatch.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.domain.model.AccountStatus
import com.golfmatch.app.domain.model.Conversation
import com.golfmatch.app.domain.model.Message
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.model.User
import com.golfmatch.app.ui.theme.GolfMatchTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * メッセージ一覧画面の1会話分の行（技術設計書4章 `ui/component`、3-2章）。
 *
 * アイコン画像の実読み込みは画像ローディングライブラリ未導入のため未実装（[UserCard]同様、
 * プレースホルダーアイコンで代替）。未読件数のバッジ表示は技術設計書に明記が無いため本実装のスコープ外
 * （タブ追加自体が可視性向上の主目的、3-3章）。
 */
@Composable
fun ConversationListItem(
    conversation: Conversation,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
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
            Text(text = conversation.partner.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = conversation.lastMessage?.content ?: "メッセージはまだありません",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formatDateTime(conversation.updatedAt),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun formatDateTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val date = local.date
    val time = local.time
    return "%02d/%02d %02d:%02d".format(date.monthNumber, date.dayOfMonth, time.hour, time.minute)
}

private fun previewConversation() = Conversation(
    conversationId = "user-1_user-2",
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

@Preview(showBackground = true)
@Composable
private fun ConversationListItemPreview() {
    GolfMatchTheme {
        ConversationListItem(conversation = previewConversation())
    }
}
