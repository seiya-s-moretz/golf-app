package com.golfmatch.app.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * ユーザーブロックの確認ダイアログ（技術設計書3-2章「ブロック確認ダイアログ」・5-2章）。
 *
 * ブロックは**確認なしで実行してはならない**（プロダクトオーナー決定、2026-08-13）。
 * ブロック中は相手の投稿・おすすめ表示だけでなく、**これまでの会話・メッセージ履歴も表示されなくなる**ため、
 * 誤操作の影響が大きい。ブロック導線（おすすめユーザー・掲示板投稿・トーク画面）が増えても文言と挙動が
 * ばらつかないよう、ダイアログを共通部品として切り出している。
 */
@Composable
fun BlockConfirmDialog(
    userName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (userName.isBlank()) "ユーザーをブロックしますか？" else "${userName}さんをブロックしますか？") },
        text = {
            Text(
                "ブロックすると、おすすめユーザー・掲示板への表示や申請・メッセージの送受信ができなくなります。" +
                    "これまでのやりとりも表示されなくなります（ブロックを解除すると元に戻ります）。"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("ブロックする") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}
