package com.golfmatch.app.ui.screen.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
    onReportPost: (BoardPost) -> Unit = {},
    onBlockAuthor: (BoardPost) -> Unit = {},
    onLoadMore: () -> Unit = {}
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
            uiState.errorMessage != null && uiState.posts.isEmpty() ->
                ErrorContent(innerPadding, uiState.errorMessage)
            uiState.posts.isEmpty() -> EmptyContent(innerPadding)
            else -> BoardPostList(innerPadding, uiState, onReportPost, onBlockAuthor, onLoadMore)
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

/** リスト末尾から何件手前で次ページの読み込みを開始するか */
private const val LOAD_MORE_THRESHOLD = 3

@Composable
private fun BoardPostList(
    padding: PaddingValues,
    uiState: BoardUiState,
    onReportPost: (BoardPost) -> Unit,
    onBlockAuthor: (BoardPost) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(uiState.posts.size, uiState.hasMore) {
        derivedStateOf {
            if (!uiState.hasMore) return@derivedStateOf false
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisibleIndex >= uiState.posts.size - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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
            items(uiState.posts, key = { it.postId }) { post ->
                BoardPostCard(
                    post = post,
                    authorName = uiState.authors[post.userId]?.name.orEmpty(),
                    modifier = Modifier.padding(bottom = 12.dp),
                    onReportClick = { onReportPost(post) },
                    onBlockUser = { onBlockAuthor(post) }
                )
            }
            if (uiState.isLoadingMore) {
                item(key = "loading-more") {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.hasMore && uiState.errorMessage != null) {
                // 追加読み込みが失敗すると末尾到達では再発火しないため、明示的な再試行導線を出す
                item(key = "load-more-retry") {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        TextButton(onClick = onLoadMore) { Text("再試行") }
                    }
                }
            }
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
