package com.golfmatch.app.ui.screen.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.domain.model.Report
import com.golfmatch.app.domain.model.ReportDetail
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.domain.model.ReportTargetUserDetail
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.ReportAdminDetailUiState
import kotlinx.datetime.Instant

/**
 * 通報管理詳細画面（管理者向け、技術設計書3-2章・7-2章・7-4章、ADR-0007）。
 *
 * 対象種別・対象ID・通報理由・通報者・現在ステータスを表示し、ステータス変更（ドロップダウン）と
 * 対応メモの入力・保存ができる。MVPでは状態遷移順序の強制は行わない（技術設計書6-9章）。
 */
@Composable
fun ReportAdminDetailScreen(
    uiState: ReportAdminDetailUiState,
    onStatusSelected: (ReportStatus) -> Unit = {},
    onHandlingMemoChange: (String) -> Unit = {},
    onSaveClick: () -> Unit = {}
) {
    Scaffold { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(innerPadding)
            uiState.errorMessage != null && uiState.report == null -> ErrorContent(innerPadding, uiState.errorMessage)
            uiState.report == null -> LoadingContent(innerPadding)
            else -> DetailForm(
                innerPadding = innerPadding,
                uiState = uiState,
                report = uiState.report,
                onStatusSelected = onStatusSelected,
                onHandlingMemoChange = onHandlingMemoChange,
                onSaveClick = onSaveClick
            )
        }
    }
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(padding: PaddingValues, message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DetailForm(
    innerPadding: PaddingValues,
    uiState: ReportAdminDetailUiState,
    report: ReportDetail,
    onStatusSelected: (ReportStatus) -> Unit,
    onHandlingMemoChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "対象種別: ${report.report.targetType.targetTypeLabel}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "対象ID: ${report.report.targetId}", style = MaterialTheme.typography.bodyMedium)

        val targetUser = report.targetUser
        val targetBoardPost = report.targetBoardPost
        if (targetUser != null) {
            Text(text = "対象ユーザー: ${targetUser.name}（${targetUser.gender} / ${targetUser.age}歳）", style = MaterialTheme.typography.bodyMedium)
            Text(text = "自己紹介: ${targetUser.introduction}", style = MaterialTheme.typography.bodySmall)
        }
        if (targetBoardPost != null) {
            Text(text = "投稿者: ${targetBoardPost.authorName}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "投稿本文: ${targetBoardPost.content}", style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider()

        Text(text = "通報理由: ${report.report.reasonCategory.label}", style = MaterialTheme.typography.bodyLarge)
        if (!report.report.reasonText.isNullOrBlank()) {
            Text(text = "理由詳細: ${report.report.reasonText}", style = MaterialTheme.typography.bodyMedium)
        }
        Text(text = "通報者: ${report.reporterName}", style = MaterialTheme.typography.bodyMedium)
        Text(text = "通報日時: ${formatDateTime(report.report.createdAt)}", style = MaterialTheme.typography.bodySmall)

        HorizontalDivider()

        Text(text = "現在のステータス: ${report.report.status.label}", style = MaterialTheme.typography.bodyLarge)

        StatusDropdown(selected = uiState.selectedStatus, onStatusSelected = onStatusSelected)

        OutlinedTextField(
            value = uiState.handlingMemo,
            onValueChange = onHandlingMemoChange,
            label = { Text("対応メモ") },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )

        if (uiState.errorMessage != null) {
            Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
        }
        if (uiState.updateSuccess) {
            Text(text = "ステータスを更新しました", color = MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = onSaveClick,
            enabled = !uiState.isUpdating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isUpdating) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("更新する")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
    selected: ReportStatus,
    onStatusSelected: (ReportStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("対応ステータス") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReportStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.label) },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

private val ReportTargetType.targetTypeLabel: String
    get() = when (this) {
        ReportTargetType.USER -> "ユーザー"
        ReportTargetType.BOARD_POST -> "掲示板投稿"
    }

private fun previewDetail() = ReportDetail(
    report = Report(
        reportId = "report-1",
        reporterUserId = "user-1",
        targetType = ReportTargetType.USER,
        targetId = "user-2",
        reasonCategory = ReportReasonCategory.HARASSMENT,
        reasonText = "しつこいメッセージを受けた",
        status = ReportStatus.PENDING,
        createdAt = Instant.parse("2026-08-10T00:00:00Z"),
        handledByUserId = null,
        handledAt = null,
        handlingMemo = null
    ),
    reporterName = "山田太郎",
    reporterIconUrl = "",
    targetUser = ReportTargetUserDetail(
        userId = "user-2",
        name = "鈴木花子",
        iconUrl = "",
        gender = "female",
        age = 28,
        introduction = "よろしくお願いします"
    )
)

@Preview(showBackground = true, name = "詳細表示")
@Composable
private fun ReportAdminDetailScreenPreview() {
    GolfMatchTheme {
        ReportAdminDetailScreen(uiState = ReportAdminDetailUiState(report = previewDetail(), reportId = "report-1"))
    }
}

@Preview(showBackground = true, name = "ローディング")
@Composable
private fun ReportAdminDetailScreenLoadingPreview() {
    GolfMatchTheme {
        ReportAdminDetailScreen(uiState = ReportAdminDetailUiState(isLoading = true))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun ReportAdminDetailScreenErrorPreview() {
    GolfMatchTheme {
        ReportAdminDetailScreen(uiState = ReportAdminDetailUiState(errorMessage = "通報詳細の取得に失敗しました"))
    }
}
