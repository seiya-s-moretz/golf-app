package com.golfmatch.app.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.domain.model.BoardPost
import com.golfmatch.app.ui.theme.GolfMatchTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 掲示板投稿カード（技術設計書4章 `ui/component`、`D:\勉強\golf\基本設計書.md` 3-3章）。
 *
 * 表示項目: 投稿者アイコン・名前・ラウンド結果（投稿内容）・投稿日時。
 * [authorName] は[BoardPost]自体には含まれない（`userId`のみ保持）ため、呼び出し側（`BoardViewModel`）が
 * `GetUserUseCase`で解決した結果を渡す（実装メモ参照）。未解決の場合は空文字列。
 */
@Composable
fun BoardPostCard(
    post: BoardPost,
    authorName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = authorName.ifEmpty { "不明なユーザー" },
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(text = formatDateTime(post.createdAt), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(text = post.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatDateTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val date = local.date
    val time = local.time
    return "%04d/%02d/%02d %02d:%02d".format(date.year, date.monthNumber, date.dayOfMonth, time.hour, time.minute)
}

@Preview(showBackground = true)
@Composable
private fun BoardPostCardPreview() {
    GolfMatchTheme {
        BoardPostCard(
            post = BoardPost(
                postId = "post-1",
                userId = "user-1",
                content = "本日ハーフ48で回れました。ベストスコア更新です！",
                createdAt = Instant.parse("2026-08-10T09:00:00Z")
            ),
            authorName = "山田太郎"
        )
    }
}
