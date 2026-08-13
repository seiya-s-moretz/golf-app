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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 *
 * 右上の「…」オーバーフローメニューから通報・ブロック導線（技術設計書7-3章）を提供する。ブロックは
 * `AlertDialog`による確認後に[onBlockUser]を呼び出す（実際の`BlockUserUseCase`呼び出しは呼び出し側
 * ViewModelの責務。UI層はビジネスロジックを持たない、技術設計書2章）。
 * 設計書4章では通報導線用の共通部品`ReportMenuItem.kt`を新設する想定だが、本カードのみの利用のため
 * 過剰な共通化を避けDropdownMenu/AlertDialogを直接組み込んだ（DeveloperAgent実装判断）。
 */
@Composable
fun UserCard(
    user: User,
    areaName: String,
    isRequested: Boolean,
    modifier: Modifier = Modifier,
    onSendMatchRequest: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onBlockUser: () -> Unit = {}
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

            UserOverflowMenu(onReportClick = onReportClick, onBlockUser = onBlockUser)
        }
    }
}

@Composable
private fun UserOverflowMenu(onReportClick: () -> Unit, onBlockUser: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "メニュー")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("通報する") },
                onClick = {
                    expanded = false
                    onReportClick()
                }
            )
            DropdownMenuItem(
                text = { Text("このユーザーをブロックする") },
                onClick = {
                    expanded = false
                    showBlockConfirm = true
                }
            )
        }
    }

    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = { Text("ユーザーをブロックしますか？") },
            // ブロック中は既存の会話・メッセージ履歴も表示されなくなる（技術設計書5-2章、2026-08-13決定）。
            // 誤操作で会話が見えなくなるため、影響をダイアログに明記してから実行する
            text = {
                Text(
                    "ブロックすると、おすすめユーザー・掲示板への表示や申請・メッセージの送受信ができなくなります。" +
                        "これまでのやりとりも表示されなくなります（ブロックを解除すると元に戻ります）。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBlockConfirm = false
                    onBlockUser()
                }) {
                    Text("ブロックする")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirm = false }) {
                    Text("キャンセル")
                }
            }
        )
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
