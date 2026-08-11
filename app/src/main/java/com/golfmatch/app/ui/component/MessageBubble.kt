package com.golfmatch.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.domain.model.Message
import com.golfmatch.app.ui.theme.GolfMatchTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * メッセージスレッド画面の1メッセージ分の吹き出し（技術設計書4章 `ui/component`、3-2章）。
 *
 * 1:1会話のため、送信者が相手（[isMine]=false）でなければ自分の発言として右寄せ表示する。
 * 既読/未読の表示制御は行わない（`read_at` 更新APIの呼び出し導線が本実装のスコープ外のため、7章の
 * `MessageThreadUiState`にも既読状態フィールドは無い）。
 */
@Composable
fun MessageBubble(
    message: Message,
    isMine: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = message.content, style = MaterialTheme.typography.bodyLarge)
            Text(text = formatTime(message.createdAt), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatTime(instant: Instant): String {
    val time = instant.toLocalDateTime(TimeZone.currentSystemDefault()).time
    return "%02d:%02d".format(time.hour, time.minute)
}

private fun previewMessage(isMine: Boolean) = Message(
    messageId = "msg-1",
    userAId = "user-1",
    userBId = "user-2",
    senderId = if (isMine) "user-1" else "user-2",
    content = "本日はありがとうございました！",
    createdAt = Instant.parse("2026-08-11T10:00:00Z"),
    readAt = null
)

@Preview(showBackground = true, name = "相手のメッセージ")
@Composable
private fun MessageBubblePartnerPreview() {
    GolfMatchTheme {
        MessageBubble(message = previewMessage(isMine = false), isMine = false)
    }
}

@Preview(showBackground = true, name = "自分のメッセージ")
@Composable
private fun MessageBubbleMinePreview() {
    GolfMatchTheme {
        MessageBubble(message = previewMessage(isMine = true), isMine = true)
    }
}
