package com.golfmatch.app.ui.screen.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.domain.model.Report
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.model.ReportSummary
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.ReportAdminListUiState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 通報管理一覧画面（管理者向け、技術設計書3-2章・7-2章・7-4章、ADR-0007）。
 *
 * `User.is_admin=true`の運営メンバーのみマイページから到達できる（一般ユーザーには存在が見えない導線）。
 * ステータスごとの簡易タブで絞り込み、行タップで通報管理詳細画面へ遷移する。
 * 一覧はサーバーのカーソル型ページネーション（技術設計書6-9章 `before`/`limit`）に従い、
 * 末尾に近づいたら[onLoadMore]で次ページを追加読み込みする。
 */
@Composable
fun ReportAdminListScreen(
    uiState: ReportAdminListUiState,
    onStatusFilterSelected: (ReportStatus?) -> Unit = {},
    onReportClick: (ReportSummary) -> Unit = {},
    onLoadMore: () -> Unit = {}
) {
    Scaffold { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            StatusFilterRow(selected = uiState.statusFilter, onStatusFilterSelected = onStatusFilterSelected)
            when {
                uiState.isLoading -> LoadingContent()
                uiState.errorMessage != null && uiState.reports.isEmpty() -> ErrorContent(uiState.errorMessage)
                uiState.reports.isEmpty() -> EmptyContent()
                else -> ReportList(uiState = uiState, onReportClick = onReportClick, onLoadMore = onLoadMore)
            }
        }
    }
}

@Composable
private fun StatusFilterRow(
    selected: ReportStatus?,
    onStatusFilterSelected: (ReportStatus?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onStatusFilterSelected(null) },
            label = { Text("すべて") }
        )
        ReportStatus.entries.forEach { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onStatusFilterSelected(status) },
                label = { Text(status.label) }
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptyContent() {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text = "該当する通報はありません", style = MaterialTheme.typography.bodyLarge)
    }
}

/** リスト末尾から何件手前で次ページの読み込みを開始するか */
private const val LOAD_MORE_THRESHOLD = 3

@Composable
private fun ReportList(
    uiState: ReportAdminListUiState,
    onReportClick: (ReportSummary) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(uiState.reports.size, uiState.hasMore) {
        derivedStateOf {
            if (!uiState.hasMore) return@derivedStateOf false
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisibleIndex >= uiState.reports.size - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(uiState.reports, key = { it.report.reportId }) { summary ->
                ReportSummaryCard(
                    summary = summary,
                    modifier = Modifier.padding(bottom = 12.dp),
                    onClick = { onReportClick(summary) }
                )
            }
            if (uiState.isLoadingMore) {
                item(key = "loading-more") { LoadMoreIndicator() }
            } else if (uiState.hasMore && uiState.errorMessage != null) {
                // 追加読み込みが失敗すると末尾到達では再発火しないため、明示的な再試行導線を出す
                item(key = "load-more-retry") { LoadMoreRetryButton(onClick = onLoadMore) }
            }
        }
    }
}

@Composable
private fun LoadMoreIndicator() {
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LoadMoreRetryButton(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
        TextButton(onClick = onClick) { Text("再試行") }
    }
}

@Composable
private fun ReportSummaryCard(
    summary: ReportSummary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = summary.report.status.label, style = MaterialTheme.typography.labelLarge)
                Text(text = formatDateTime(summary.report.createdAt), style = MaterialTheme.typography.labelSmall)
            }
            Text(text = "対象: ${summary.targetSummary}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "通報者: ${summary.reporterName}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "理由: ${summary.report.reasonCategory.label}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

internal val ReportStatus.label: String
    get() = when (this) {
        ReportStatus.PENDING -> "未対応"
        ReportStatus.REVIEWING -> "対応中"
        ReportStatus.RESOLVED -> "対応済み"
        ReportStatus.DISMISSED -> "却下"
    }

internal val ReportReasonCategory.label: String
    get() = when (this) {
        ReportReasonCategory.SPAM -> "スパム"
        ReportReasonCategory.DATING_SOLICITATION -> "出会い目的利用"
        ReportReasonCategory.HARASSMENT -> "嫌がらせ"
        ReportReasonCategory.INAPPROPRIATE_CONTENT -> "不適切なコンテンツ"
        ReportReasonCategory.OTHER -> "その他"
    }

internal fun formatDateTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val date = local.date
    val time = local.time
    return "%04d/%02d/%02d %02d:%02d".format(date.year, date.monthNumber, date.dayOfMonth, time.hour, time.minute)
}

private fun previewReports() = listOf(
    ReportSummary(
        report = Report(
            reportId = "report-1",
            reporterUserId = "user-1",
            targetType = ReportTargetType.USER,
            targetId = "user-2",
            reasonCategory = ReportReasonCategory.HARASSMENT,
            reasonText = null,
            status = ReportStatus.PENDING,
            createdAt = Instant.parse("2026-08-10T00:00:00Z"),
            handledByUserId = null,
            handledAt = null,
            handlingMemo = null
        ),
        reporterName = "山田太郎",
        reporterIconUrl = "",
        targetSummary = "鈴木花子"
    )
)

@Preview(showBackground = true, name = "一覧表示")
@Composable
private fun ReportAdminListScreenPreview() {
    GolfMatchTheme {
        ReportAdminListScreen(uiState = ReportAdminListUiState(reports = previewReports()))
    }
}

@Preview(showBackground = true, name = "空状態")
@Composable
private fun ReportAdminListScreenEmptyPreview() {
    GolfMatchTheme {
        ReportAdminListScreen(uiState = ReportAdminListUiState(reports = emptyList()))
    }
}

@Preview(showBackground = true, name = "ローディング")
@Composable
private fun ReportAdminListScreenLoadingPreview() {
    GolfMatchTheme {
        ReportAdminListScreen(uiState = ReportAdminListUiState(isLoading = true))
    }
}
