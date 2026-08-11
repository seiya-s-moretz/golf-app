package com.golfmatch.app.ui.screen.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.CreateBoardPostUiState

/**
 * 掲示板新規投稿画面（技術設計書3-1章、`D:\勉強\golf\基本設計書.md` 3-3章）。
 *
 * PRD 3-1章のとおり投稿はテキストのみ（画像投稿は将来検討）。
 */
@Composable
fun CreateBoardPostScreen(
    uiState: CreateBoardPostUiState,
    onContentChange: (String) -> Unit = {},
    onSubmitClick: () -> Unit = {}
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.content,
                onValueChange = onContentChange,
                label = { Text("ラウンド結果など") },
                placeholder = { Text("例: 本日ハーフ48で回れました") },
                modifier = Modifier.fillMaxWidth().height(160.dp)
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
                    Text("投稿する")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "入力")
@Composable
private fun CreateBoardPostScreenPreview() {
    GolfMatchTheme {
        CreateBoardPostScreen(uiState = CreateBoardPostUiState(content = "本日ハーフ48で回れました"))
    }
}

@Preview(showBackground = true, name = "送信中")
@Composable
private fun CreateBoardPostScreenSubmittingPreview() {
    GolfMatchTheme {
        CreateBoardPostScreen(uiState = CreateBoardPostUiState(content = "本日ハーフ48で回れました", isSubmitting = true))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun CreateBoardPostScreenErrorPreview() {
    GolfMatchTheme {
        CreateBoardPostScreen(uiState = CreateBoardPostUiState(errorMessage = "投稿内容を入力してください"))
    }
}
