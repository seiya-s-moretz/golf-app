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
import androidx.compose.material3.Button
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
import com.golfmatch.app.domain.model.AccountStatus
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.model.User
import com.golfmatch.app.ui.theme.GolfMatchTheme
import kotlinx.datetime.Instant

/**
 * おすすめユーザーカード（技術設計書4章 `ui/component`、`D:\勉強\golf\基本設計書.md` 3-2章）。
 *
 * 表示項目: アイコン・名前・アベレージスコア・住居エリア・目的。
 * アイコン画像（[User.iconUrl]）の実際の読み込みは画像ローディングライブラリ未導入のため次フェーズとし、
 * 現時点ではプレースホルダーアイコンを表示する（既存の`RoundEventCard`同様、画像表示は未実装のまま踏襲）。
 */
@Composable
fun UserCard(
    user: User,
    areaName: String,
    isRequested: Boolean,
    modifier: Modifier = Modifier,
    onSendMatchRequest: () -> Unit = {}
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
                Text(text = "平均スコア ${user.averageScore}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = areaName.ifEmpty { "エリア未設定" },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(text = "目的: ${user.purpose.label}", style = MaterialTheme.typography.bodyMedium)
            }

            Button(onClick = onSendMatchRequest, enabled = !isRequested) {
                Text(text = if (isRequested) "申請済み" else "申請する")
            }
        }
    }
}

private fun previewUser() = User(
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
)

@Preview(showBackground = true, name = "未申請")
@Composable
private fun UserCardPreview() {
    GolfMatchTheme {
        UserCard(user = previewUser(), areaName = "さいたま市", isRequested = false)
    }
}

@Preview(showBackground = true, name = "申請済み")
@Composable
private fun UserCardRequestedPreview() {
    GolfMatchTheme {
        UserCard(user = previewUser(), areaName = "さいたま市", isRequested = true)
    }
}
