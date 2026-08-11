package com.golfmatch.app.ui.screen.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.ReportUiState

/**
 * 通報画面（技術設計書3-2章・7-2章・7-3章）。
 *
 * 掲示板投稿詳細・ユーザー詳細画面の「…」メニューから遷移し、通報理由（[ReportReasonCategory]、5値）を
 * 選択のうえ自由記述（`reasonText`、`OTHER`選択時は必須）を入力して送信する。
 * 技術設計書4章では`report/ReportDialog.kt`という配置になっているが、[Route.Report][com.golfmatch.app.ui.navigation.Route.Report]
 * は独立したNavGraph destinationとして既に定義済みのため、他画面と同じ`XxxScreen.kt`命名規則に揃えて
 * `ReportScreen.kt`とした（DeveloperAgent実装判断）。
 */
@Composable
fun ReportScreen(
    uiState: ReportUiState,
    onReasonCategorySelected: (ReportReasonCategory) -> Unit = {},
    onReasonTextChange: (String) -> Unit = {},
    onSubmitClick: () -> Unit = {}
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (uiState.targetType == ReportTargetType.BOARD_POST) "投稿を通報する" else "ユーザーを通報する",
                style = MaterialTheme.typography.titleLarge
            )

            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ReportReasonCategory.entries.forEach { category ->
                    ReasonOption(
                        label = category.label,
                        selected = uiState.reasonCategory == category,
                        onClick = { onReasonCategorySelected(category) }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.reasonText,
                onValueChange = onReasonTextChange,
                label = { Text("詳細（任意。「その他」選択時は必須）") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
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
                    Text("通報する")
                }
            }
        }
    }
}

@Composable
private fun ReasonOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private val ReportReasonCategory.label: String
    get() = when (this) {
        ReportReasonCategory.SPAM -> "スパム・宣伝"
        ReportReasonCategory.DATING_SOLICITATION -> "出会い目的の利用"
        ReportReasonCategory.HARASSMENT -> "嫌がらせ・誹謗中傷"
        ReportReasonCategory.INAPPROPRIATE_CONTENT -> "不適切なコンテンツ"
        ReportReasonCategory.OTHER -> "その他"
    }

@Preview(showBackground = true, name = "入力")
@Composable
private fun ReportScreenPreview() {
    GolfMatchTheme {
        ReportScreen(
            uiState = ReportUiState(
                targetType = ReportTargetType.BOARD_POST,
                targetId = "post-1",
                reasonCategory = ReportReasonCategory.SPAM
            )
        )
    }
}

@Preview(showBackground = true, name = "送信中")
@Composable
private fun ReportScreenSubmittingPreview() {
    GolfMatchTheme {
        ReportScreen(
            uiState = ReportUiState(
                targetType = ReportTargetType.USER,
                targetId = "user-1",
                reasonCategory = ReportReasonCategory.HARASSMENT,
                isSubmitting = true
            )
        )
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun ReportScreenErrorPreview() {
    GolfMatchTheme {
        ReportScreen(
            uiState = ReportUiState(
                targetType = ReportTargetType.USER,
                targetId = "user-1",
                errorMessage = "通報理由を選択してください"
            )
        )
    }
}
