package com.golfmatch.app.ui.viewmodel

import com.golfmatch.app.domain.model.BoardPost
import com.golfmatch.app.domain.repository.BoardRepository
import com.golfmatch.app.domain.usecase.BlockUserUseCase
import com.golfmatch.app.domain.usecase.GetBoardPostsUseCase
import com.golfmatch.app.domain.usecase.GetUserUseCase
import com.golfmatch.app.testutil.FakeBoardRepository
import com.golfmatch.app.testutil.FakeUserRepository
import com.golfmatch.app.testutil.MainDispatcherRule
import com.golfmatch.app.testutil.TestFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 掲示板画面ViewModelのテスト（技術設計書6-6章）。
 *
 * 掲示板はカーソル型ページネーション（`before`/`limit`）に対応している。
 * ブロック除外がサーバー側で「取得後」に行われるため、**1ページの件数が`limit`未満でも
 * 次ページが存在しうる**点が他の一覧と異なり、終端判定を取り違えると投稿が読めなくなる。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BoardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val pageSize = 20

    private fun viewModel(repo: BoardRepository) = BoardViewModel(
        GetBoardPostsUseCase(repo),
        GetUserUseCase(FakeUserRepository()),
        BlockUserUseCase(FakeUserRepository())
    )

    @Test
    fun `初回はカーソルなしでページサイズ分を取得する`() = runTest {
        val repo = PagingBoardRepository(pages = listOf(posts(0, pageSize)))

        val state = viewModel(repo).uiState.value

        assertEquals(1, repo.calls.size)
        assertEquals(Triple(null, null, pageSize), repo.calls[0])
        assertEquals(pageSize, state.posts.size)
        assertTrue(state.hasMore)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadMoreは末尾のcreated_atとIDをカーソルに次ページを末尾へ追加する`() = runTest {
        val firstPage = posts(0, pageSize)
        val repo = PagingBoardRepository(pages = listOf(firstPage, posts(pageSize, 5)))
        val viewModel = viewModel(repo)

        viewModel.loadMore()

        // 時刻だけだと同時刻の投稿がページ境界で取りこぼされるため、IDも一緒に渡す
        assertEquals(
            Triple(firstPage.last().createdAt.toString(), firstPage.last().postId, pageSize),
            repo.calls[1]
        )
        val state = viewModel.uiState.value
        assertEquals(pageSize + 5, state.posts.size)
        assertEquals("post-${pageSize + 4}", state.posts.last().postId)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `1件も返らなくなるまで次ページがあるとみなす（ブロック除外で件数が減るため）`() = runTest {
        // 2ページ目はブロック除外により1件しか返らないが、まだ続きがあるかもしれない
        val repo = PagingBoardRepository(pages = listOf(posts(0, pageSize), posts(pageSize, 1), emptyList()))
        val viewModel = viewModel(repo)

        viewModel.loadMore()
        assertTrue("件数が少なくても終端とみなしてはいけない", viewModel.uiState.value.hasMore)

        viewModel.loadMore()
        assertFalse(viewModel.uiState.value.hasMore)
        assertEquals(3, repo.calls.size)
    }

    @Test
    fun `次ページが無い状態でloadMoreを呼んでもAPIを呼ばない`() = runTest {
        val repo = PagingBoardRepository(pages = listOf(posts(0, 3)))
        val viewModel = viewModel(repo)

        assertFalse(viewModel.uiState.value.hasMore)
        viewModel.loadMore()

        assertEquals(1, repo.calls.size)
    }

    @Test
    fun `loadMore失敗時は取得済みの一覧とhasMoreを維持したままエラーを表示する`() = runTest {
        val repo = PagingBoardRepository(pages = listOf(posts(0, pageSize)), failOnCall = 2)
        val viewModel = viewModel(repo)

        viewModel.loadMore()

        val state = viewModel.uiState.value
        assertEquals("取得失敗", state.errorMessage)
        assertEquals(pageSize, state.posts.size)
        assertTrue(state.hasMore)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `投稿者をブロックするとその投稿が一覧から即時に消える`() = runTest {
        val repo = PagingBoardRepository(
            pages = listOf(
                listOf(
                    TestFixtures.boardPost(postId = "post-1", userId = "user-1"),
                    TestFixtures.boardPost(postId = "post-2", userId = "user-2"),
                    TestFixtures.boardPost(postId = "post-3", userId = "user-1")
                )
            )
        )
        val userRepo = FakeUserRepository()
        val viewModel = BoardViewModel(
            GetBoardPostsUseCase(repo),
            GetUserUseCase(userRepo),
            BlockUserUseCase(userRepo)
        )

        viewModel.blockUser("user-1")

        assertEquals("user-1", userRepo.lastBlockedUserId)
        // サーバー側の一覧もブロック相手を除外するが、再取得を待たずに手元から消す
        assertEquals(listOf("post-2"), viewModel.uiState.value.posts.map { it.postId })
    }

    /** `created_at`降順の連番ダミーデータ（index順に古くなる） */
    private fun posts(fromIndex: Int, count: Int): List<BoardPost> = List(count) { offset ->
        val index = fromIndex + offset
        TestFixtures.boardPost(postId = "post-$index")
            .copy(createdAt = Instant.fromEpochSeconds(1_800_000_000L - index))
    }

    private class PagingBoardRepository(
        private val pages: List<List<BoardPost>>,
        private val failOnCall: Int? = null
    ) : BoardRepository by FakeBoardRepository() {

        /** (before, before_id, limit) の呼び出し履歴 */
        val calls = mutableListOf<Triple<String?, String?, Int>>()

        override suspend fun getBoardPosts(before: String?, beforeId: String?, limit: Int): List<BoardPost> {
            calls += Triple(before, beforeId, limit)
            if (failOnCall == calls.size) throw RuntimeException("取得失敗")
            return pages.getOrElse(calls.size - 1) { emptyList() }
        }
    }
}
