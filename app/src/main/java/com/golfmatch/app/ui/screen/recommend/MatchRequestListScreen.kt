package com.golfmatch.app.ui.screen.recommend

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.domain.model.MatchRequest
import com.golfmatch.app.domain.model.MatchRequestStatus
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.MatchRequestListUiState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 受信マッチング申請一覧画面（技術設計書3-2章・7-2章）。
 *
 * 自分宛（`to_user_id`）の申請のみを対象とし、`PENDING`状態の申請にのみ承認・却下ボタンを表示する
 * （6-5章 `POST /match-requests/{id}/approve` の認可は`to_user_id`本人のみ）。
 */
@Composable
fun MatchRequestListScreen(
    uiState: MatchRequestListUiState,
    onApproveClick: (MatchRequest) -> Unit = {},
    onRejectClick: (MatchRequest) -> Unit = {}
) {
    Scaffold { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(innerPadding)
            uiState.errorMessage != null && uiState.receivedRequests.isEmpty() ->
                ErrorContent(innerPadding, uiState.errorMessage)
            uiState.receivedRequests.isEmpty() -> EmptyContent(innerPadding)
            else -> RequestList(innerPadding, uiState, onApproveClick, onRejectClick)
        }
    }
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(padding: PaddingValues, message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EmptyContent(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "受信したマッチング申請はまだありません", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RequestList(
    padding: PaddingValues,
    uiState: MatchRequestListUiState,
    onApproveClick: (MatchRequest) -> Unit,
    onRejectClick: (MatchRequest) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(uiState.receivedRequests, key = { it.matchRequestId }) { request ->
                MatchRequestCard(
                    request = request,
                    isProcessing = uiState.processingRequestId == request.matchRequestId,
                    modifier = Modifier.padding(bottom = 12.dp),
                    onApproveClick = { onApproveClick(request) },
                    onRejectClick = { onRejectClick(request) }
                )
            }
        }
    }
}

@Composable
private fun MatchRequestCard(
    request: MatchRequest,
    isProcessing: Boolean,
    modifier: Modifier = Modifier,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "申請者ID: ${request.fromUserId}", style = MaterialTheme.typography.titleMedium)
            Text(text = "申請日時: ${formatDateTime(request.createdAt)}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "状態: ${request.status.label}", style = MaterialTheme.typography.bodyMedium)

            if (request.status == MatchRequestStatus.PENDING) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onApproveClick, enabled = !isProcessing) {
                        Text("承認")
                    }
                    OutlinedButton(onClick = onRejectClick, enabled = !isProcessing) {
                        Text("却下")
                    }
                }
            }
        }
    }
}

private val MatchRequestStatus.label: String
    get() = when (this) {
        MatchRequestStatus.PENDING -> "申請中"
        MatchRequestStatus.ACCEPTED -> "承認済み"
        MatchRequestStatus.REJECTED -> "却下済み"
    }

private fun formatDateTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val date = local.date
    val time = local.time
    return "%04d/%02d/%02d %02d:%02d".format(date.year, date.monthNumber, date.dayOfMonth, time.hour, time.minute)
}

private fun previewRequests() = listOf(
    MatchRequest(
        matchRequestId = "match-req-1",
        fromUserId = "user-2",
        toUserId = "user-1",
        status = MatchRequestStatus.PENDING,
        createdAt = Instant.parse("2026-08-10T00:00:00Z"),
        respondedAt = null
    ),
    MatchRequest(
        matchRequestId = "match-req-2",
        fromUserId = "user-3",
        toUserId = "user-1",
        status = MatchRequestStatus.ACCEPTED,
        createdAt = Instant.parse("2026-08-09T00:00:00Z"),
        respondedAt = Instant.parse("2026-08-09T01:00:00Z")
    )
)

@Preview(showBackground = true, name = "一覧表示")
@Composable
private fun MatchRequestListScreenPreview() {
    GolfMatchTheme {
        MatchRequestListScreen(uiState = MatchRequestListUiState(receivedRequests = previewRequests()))
    }
}

@Preview(showBackground = true, name = "ローディング")
@Composable
private fun MatchRequestListScreenLoadingPreview() {
    GolfMatchTheme {
        MatchRequestListScreen(uiState = MatchRequestListUiState(isLoading = true))
    }
}

@Preview(showBackground = true, name = "空状態")
@Composable
private fun MatchRequestListScreenEmptyPreview() {
    GolfMatchTheme {
        MatchRequestListScreen(uiState = MatchRequestListUiState(receivedRequests = emptyList()))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun MatchRequestListScreenErrorPreview() {
    GolfMatchTheme {
        MatchRequestListScreen(uiState = MatchRequestListUiState(errorMessage = "マッチング申請一覧の取得に失敗しました"))
    }
}
