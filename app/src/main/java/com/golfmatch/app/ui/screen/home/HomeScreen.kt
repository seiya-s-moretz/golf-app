package com.golfmatch.app.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.golfmatch.app.domain.model.RoundEvent
import com.golfmatch.app.ui.component.RoundEventCard
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.HomeUiState
import kotlinx.datetime.Instant

/**
 * ホーム（ラウンド募集一覧）画面（技術設計書3-1章、`D:\勉強\golf\基本設計書.md` 3-1章）。
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onRoundEventClick: (RoundEvent) -> Unit = {},
    onCreateRoundClick: () -> Unit = {}
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRoundClick) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "ラウンド募集を新規作成")
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(innerPadding)
            uiState.errorMessage != null -> ErrorContent(innerPadding, uiState.errorMessage)
            uiState.roundEvents.isEmpty() -> EmptyContent(innerPadding)
            else -> RoundEventList(innerPadding, uiState.roundEvents, onRoundEventClick)
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
        Text(text = "募集中のラウンドはまだありません", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RoundEventList(
    padding: PaddingValues,
    roundEvents: List<RoundEvent>,
    onRoundEventClick: (RoundEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(roundEvents, key = { it.eventId }) { roundEvent ->
            RoundEventCard(
                roundEvent = roundEvent,
                modifier = Modifier.padding(bottom = 12.dp),
                onClick = { onRoundEventClick(roundEvent) }
            )
        }
    }
}

private fun previewRoundEvents() = listOf(
    RoundEvent(
        eventId = "event-1",
        clubName = "さいたまゴルフ倶楽部",
        datetime = Instant.parse("2026-09-01T08:00:00Z"),
        fee = 8000,
        capacity = 4,
        current = 2,
        createdBy = "user-1",
        createdAt = Instant.parse("2026-08-01T00:00:00Z")
    ),
    RoundEvent(
        eventId = "event-2",
        clubName = "川口カントリークラブ",
        datetime = Instant.parse("2026-09-10T06:30:00Z"),
        fee = 12000,
        capacity = 4,
        current = 1,
        createdBy = "user-2",
        createdAt = Instant.parse("2026-08-02T00:00:00Z")
    )
)

@Preview(showBackground = true, name = "一覧表示")
@Composable
private fun HomeScreenPreview() {
    GolfMatchTheme {
        HomeScreen(uiState = HomeUiState(roundEvents = previewRoundEvents()))
    }
}

@Preview(showBackground = true, name = "ローディング")
@Composable
private fun HomeScreenLoadingPreview() {
    GolfMatchTheme {
        HomeScreen(uiState = HomeUiState(isLoading = true))
    }
}

@Preview(showBackground = true, name = "空状態")
@Composable
private fun HomeScreenEmptyPreview() {
    GolfMatchTheme {
        HomeScreen(uiState = HomeUiState(roundEvents = emptyList()))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun HomeScreenErrorPreview() {
    GolfMatchTheme {
        HomeScreen(uiState = HomeUiState(errorMessage = "ラウンド募集一覧の取得に失敗しました"))
    }
}
