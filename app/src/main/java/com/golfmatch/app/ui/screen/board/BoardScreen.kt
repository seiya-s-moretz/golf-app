package com.golfmatch.app.ui.screen.board

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
import com.golfmatch.app.domain.model.BoardPost
import com.golfmatch.app.ui.component.BoardPostCard
import com.golfmatch.app.ui.theme.GolfMatchTheme
import com.golfmatch.app.ui.viewmodel.BoardUiState
import kotlinx.datetime.Instant

/**
 * 掲示板（投稿一覧）画面（技術設計書3-1章、`D:\勉強\golf\基本設計書.md` 3-3章）。
 */
@Composable
fun BoardScreen(
    uiState: BoardUiState,
    onCreatePostClick: () -> Unit = {},
    onReportPost: (BoardPost) -> Unit = {}
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreatePostClick) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "新規投稿")
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(innerPadding)
            uiState.errorMessage != null -> ErrorContent(innerPadding, uiState.errorMessage)
            uiState.posts.isEmpty() -> EmptyContent(innerPadding)
            else -> BoardPostList(innerPadding, uiState, onReportPost)
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
        Text(text = "投稿はまだありません", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BoardPostList(padding: PaddingValues, uiState: BoardUiState, onReportPost: (BoardPost) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(uiState.posts, key = { it.postId }) { post ->
            BoardPostCard(
                post = post,
                authorName = uiState.authors[post.userId]?.name.orEmpty(),
                modifier = Modifier.padding(bottom = 12.dp),
                onReportClick = { onReportPost(post) }
            )
        }
    }
}

private fun previewPosts() = listOf(
    BoardPost(
        postId = "post-1",
        userId = "user-1",
        content = "本日ハーフ48で回れました。ベストスコア更新です！",
        createdAt = Instant.parse("2026-08-10T09:00:00Z")
    ),
    BoardPost(
        postId = "post-2",
        userId = "user-2",
        content = "初めてのラウンドでした。皆さん優しくて楽しかったです。",
        createdAt = Instant.parse("2026-08-09T12:00:00Z")
    )
)

@Preview(showBackground = true, name = "一覧表示")
@Composable
private fun BoardScreenPreview() {
    GolfMatchTheme {
        BoardScreen(uiState = BoardUiState(posts = previewPosts()))
    }
}

@Preview(showBackground = true, name = "ローディング")
@Composable
private fun BoardScreenLoadingPreview() {
    GolfMatchTheme {
        BoardScreen(uiState = BoardUiState(isLoading = true))
    }
}

@Preview(showBackground = true, name = "空状態")
@Composable
private fun BoardScreenEmptyPreview() {
    GolfMatchTheme {
        BoardScreen(uiState = BoardUiState(posts = emptyList()))
    }
}

@Preview(showBackground = true, name = "エラー")
@Composable
private fun BoardScreenErrorPreview() {
    GolfMatchTheme {
        BoardScreen(uiState = BoardUiState(errorMessage = "掲示板投稿一覧の取得に失敗しました"))
    }
}
