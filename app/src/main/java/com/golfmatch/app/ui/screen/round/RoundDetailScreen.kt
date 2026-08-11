package com.golfmatch.app.ui.screen.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.golfmatch.app.domain.model.RoundEvent
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.RoundDetailUiState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ラウンド詳細画面（技術設計書3-1章、`D:\勉強\golf\基本設計書.md` 3-1章）。
 *
 * 募集詳細の表示と参加申請（[onApplyClick]）を行う。自分が主催者の場合
 * （[RoundDetailUiState.isOrganizer]）は参加申請一覧画面への導線を表示する（ADR-0001）。
 */
@Composable
fun RoundDetailScreen(
    uiState: RoundDetailUiState,
    onApplyClick: () -> Unit = {},
    onViewJoinRequestsClick: () -> Unit = {}
) {
    Scaffold { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(innerPadding)
            uiState.errorMessage != null && uiState.roundEvent == null -> ErrorContent(innerPadding, uiState.errorMessage)
            uiState.roundEvent != null -> DetailContent(innerPadding, uiState, onApplyClick, onViewJoinRequestsClick)
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
private fun DetailContent(
    padding: PaddingValues,
    uiState: RoundDetailUiState,
    onApplyClick: () -> Unit,
    onViewJoinRequestsClick: () -> Unit
) {
    val roundEvent = requireNotNull(uiState.roundEvent)

    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = roundEvent.clubName, style = MaterialTheme.typography.headlineSmall)
        Text(text = formatDateTime(roundEvent.datetime), style = MaterialTheme.typography.bodyLarge)
        Text(text = "%,d円".format(roundEvent.fee), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "募集人数 ${roundEvent.current}人 / ${roundEvent.capacity}人",
            style = MaterialTheme.typography.bodyLarge
        )

        if (uiState.errorMessage != null) {
            Text(text = uiState.errorMessage, color = MaterialTheme.colorScheme.error)
        }

        if (uiState.isOrganizer) {
            OutlinedButton(onClick = onViewJoinRequestsClick, modifier = Modifier.fillMaxWidth()) {
                Text("参加申請一覧を見る")
            }
        } else {
            Button(
                onClick = onApplyClick,
                enabled = !uiState.isApplying && !uiState.applySuccess,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isApplying) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(if (uiState.applySuccess) "参加申請済み" else "参加を申請する")
                }
            }
        }
    }
}

private fun formatDateTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val date = local.date
    val time = local.time
    return "%04d/%02d/%02d %02d:%02d".format(date.year, date.monthNumber, date.dayOfMonth, time.hour, time.minute)
}

private fun previewRoundEvent(current: Int = 2) = RoundEvent(
    eventId = "event-1",
    clubName = "さいたまゴルフ倶楽部",
    datetime = Instant.parse("2026-09-01T08:00:00Z"),
    fee = 8000,
    capacity = 4,
    current = current,
    createdBy = "user-1",
    createdAt = Instant.parse("2026-08-01T00:00:00Z")
)

@Preview(showBackground = true, name = "参加者視点")
@Composable
private fun RoundDetailScreenPreview() {
    GolfMatchTheme {
        RoundDetailScreen(uiState = RoundDetailUiState(roundEvent = previewRoundEvent(), isOrganizer = false))
    }
}

@Preview(showBackground = true, name = "主催者視点")
@Composable
private fun RoundDetailScreenOrganizerPreview() {
    GolfMatchTheme {
        RoundDetailScreen(uiState = RoundDetailUiState(roundEvent = previewRoundEvent(), isOrganizer = true))
    }
}

@Preview(showBackground = true, name = "ローディング")
@Composable
private fun RoundDetailScreenLoadingPreview() {
    GolfMatchTheme {
        RoundDetailScreen(uiState = RoundDetailUiState(isLoading = true))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun RoundDetailScreenErrorPreview() {
    GolfMatchTheme {
        RoundDetailScreen(uiState = RoundDetailUiState(errorMessage = "ラウンド募集詳細の取得に失敗しました"))
    }
}
