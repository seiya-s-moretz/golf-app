package com.golfmatch.app.ui.screen.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.CreateRoundUiState

/**
 * ラウンド新規作成画面（技術設計書3-1章、`D:\勉強\golf\基本設計書.md` 3-1章）。
 *
 * 入力項目: ゴルフ倶楽部名・日時・費用・募集人数（技術設計書5-1章 RoundEvent、`UiState定義.md` 2章）。
 * 日時はISOローカル日時形式（例: `2026-09-01T08:00`）での入力を前提とする
 * （日付・時刻選択UIコンポーネントは未導入のため、既存画面と同粒度のテキスト入力に留める実装判断）。
 */
@Composable
fun CreateRoundScreen(
    uiState: CreateRoundUiState,
    onClubNameChange: (String) -> Unit = {},
    onDateTimeChange: (String) -> Unit = {},
    onFeeChange: (String) -> Unit = {},
    onCapacityChange: (String) -> Unit = {},
    onSubmitClick: () -> Unit = {}
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.clubName,
                onValueChange = onClubNameChange,
                label = { Text("ゴルフ倶楽部名") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.dateTime,
                onValueChange = onDateTimeChange,
                label = { Text("日時") },
                placeholder = { Text("例: 2026-09-01T08:00") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.fee,
                onValueChange = onFeeChange,
                label = { Text("費用（円）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.capacity,
                onValueChange = onCapacityChange,
                label = { Text("募集人数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.errorMessage != null) {
                Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = onSubmitClick,
                enabled = !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("募集を作成する")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "入力")
@Composable
private fun CreateRoundScreenPreview() {
    GolfMatchTheme {
        CreateRoundScreen(
            uiState = CreateRoundUiState(
                clubName = "さいたまゴルフ倶楽部",
                dateTime = "2026-09-01T08:00",
                fee = "8000",
                capacity = "4"
            )
        )
    }
}

@Preview(showBackground = true, name = "送信中")
@Composable
private fun CreateRoundScreenSubmittingPreview() {
    GolfMatchTheme {
        CreateRoundScreen(uiState = CreateRoundUiState(clubName = "さいたまゴルフ倶楽部", isSubmitting = true))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun CreateRoundScreenErrorPreview() {
    GolfMatchTheme {
        CreateRoundScreen(uiState = CreateRoundUiState(errorMessage = "入力内容を確認してください"))
    }
}
