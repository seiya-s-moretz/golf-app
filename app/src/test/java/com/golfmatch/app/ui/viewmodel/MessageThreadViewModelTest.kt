package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.golfmatch.app.domain.model.Message
import com.golfmatch.app.domain.repository.MessageRepository
import com.golfmatch.app.domain.usecase.GetMessagesUseCase
import com.golfmatch.app.domain.usecase.GetUserUseCase
import com.golfmatch.app.domain.usecase.SendMessageUseCase
import com.golfmatch.app.testutil.FakeMessageRepository
import com.golfmatch.app.testutil.FakeUserRepository
import com.golfmatch.app.testutil.MainDispatcherRule
import com.golfmatch.app.testutil.TestFixtures
import com.golfmatch.app.ui.navigation.Route
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * メッセージスレッド画面ViewModelのテスト（技術設計書6-7章 `GET /conversations/{partnerId}/messages`）。
 *
 * サーバーは`created_at`降順（新しい順）で返すため、ViewModelが古い順（末尾が最新）に反転して保持すること、
 * および`before`カーソルによる過去メッセージの遡り読み込みを重点的に検証する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageThreadViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** ViewModelのPAGE_SIZE */
    private val pageSize = 50
    private val partnerId = "user-2"

    private fun savedStateHandle() =
        SavedStateHandle(mapOf(Route.MessageThread.ARG_PARTNER_ID to partnerId))

    private fun viewModel(repo: MessageRepository) = MessageThreadViewModel(
        savedStateHandle(),
        GetMessagesUseCase(repo),
        SendMessageUseCase(repo),
        GetUserUseCase(FakeUserRepository())
    )

    @Test
    fun `loadはAPIの新しい順を反転し古い順(末尾が最新)で保持する`() = runTest {
        // APIは新しい順で返す
        val repo = PagingMessageRepository(pages = listOf(newestFirst(0, 3)))

        val state = viewModel(repo).uiState.value

        assertEquals(listOf("message-2", "message-1", "message-0"), state.messages.map { it.messageId })
        assertEquals(Triple(partnerId, null, pageSize), repo.calls[0])
        assertFalse(state.isLoading)
    }

    @Test
    fun `1ページ分ちょうど取得できたらhasOlderが立つ`() = runTest {
        val repo = PagingMessageRepository(pages = listOf(newestFirst(0, pageSize)))

        assertTrue(viewModel(repo).uiState.value.hasOlder)
    }

    @Test
    fun `loadOlderは先頭(最古)のcreated_atをカーソルに過去を先頭へ追加する`() = runTest {
        val firstPage = newestFirst(0, pageSize)
        val repo = PagingMessageRepository(pages = listOf(firstPage, newestFirst(pageSize, 5)))
        val viewModel = viewModel(repo)

        viewModel.loadOlder()

        // カーソルは画面上の先頭＝APIレスポンス末尾（最も古い）メッセージの時刻
        assertEquals(
            Triple(partnerId, firstPage.last().createdAt.toString(), pageSize),
            repo.calls[1]
        )
        val state = viewModel.uiState.value
        assertEquals(pageSize + 5, state.messages.size)
        // 追加された5件が先頭（より古い側）に、既存の最古メッセージがその直後に並ぶ
        assertEquals("message-${pageSize + 4}", state.messages.first().messageId)
        assertEquals("message-${pageSize - 1}", state.messages[5].messageId)
        // 末尾（最新）は変わらない
        assertEquals("message-0", state.messages.last().messageId)
        assertFalse(state.hasOlder)
        assertFalse(state.isLoadingOlder)
    }

    @Test
    fun `1ページ目がPAGE_SIZE未満ならloadOlderしてもAPIを呼ばない`() = runTest {
        val repo = PagingMessageRepository(pages = listOf(newestFirst(0, 3)))
        val viewModel = viewModel(repo)

        assertFalse(viewModel.uiState.value.hasOlder)

        viewModel.loadOlder()

        assertEquals(1, repo.calls.size)
    }

    @Test
    fun `loadOlder実行中に再度呼んでもAPIは二重に呼ばれない(上端到達の多重発火対策)`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repo = PagingMessageRepository(
            pages = listOf(newestFirst(0, pageSize), newestFirst(pageSize, 5)),
            gateOnCursor = gate
        )
        val viewModel = viewModel(repo)

        viewModel.loadOlder()
        assertTrue(viewModel.uiState.value.isLoadingOlder)

        viewModel.loadOlder()
        gate.complete(Unit)

        assertEquals(2, repo.calls.size)
        assertEquals(pageSize + 5, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `loadOlder失敗時は取得済みの履歴とhasOlderを維持したままエラーを表示する`() = runTest {
        val repo = PagingMessageRepository(pages = listOf(newestFirst(0, pageSize)), failOnCall = 2)
        val viewModel = viewModel(repo)

        viewModel.loadOlder()

        val state = viewModel.uiState.value
        assertEquals("取得失敗", state.errorMessage)
        assertEquals(pageSize, state.messages.size)
        assertTrue(state.hasOlder)
        assertFalse(state.isLoadingOlder)
    }

    @Test
    fun `送信したメッセージは最新として末尾に追加される`() = runTest {
        val repo = PagingMessageRepository(pages = listOf(newestFirst(0, 3)))
        val viewModel = viewModel(repo)

        viewModel.onInputTextChange("よろしくお願いします")
        viewModel.send()

        val messages = viewModel.uiState.value.messages
        assertEquals(4, messages.size)
        assertEquals("よろしくお願いします", messages.last().content)
        assertEquals("", viewModel.uiState.value.inputText)
    }

    /** APIレスポンス相当の新しい順（`created_at`降順）メッセージ列。index が大きいほど古い */
    private fun newestFirst(fromIndex: Int, count: Int): List<Message> = List(count) { offset ->
        val index = fromIndex + offset
        TestFixtures.message(messageId = "message-$index")
            .copy(createdAt = Instant.fromEpochSeconds(1_800_000_000L - index))
    }

    /**
     * ページごとの応答を返し、呼び出し引数を記録するMessageRepository。
     * [gateOnCursor]を渡すとカーソル付き（＝過去の追加読み込み）の呼び出しだけを一時停止できる。
     */
    private class PagingMessageRepository(
        private val pages: List<List<Message>>,
        private val failOnCall: Int? = null,
        private val gateOnCursor: CompletableDeferred<Unit>? = null
    ) : MessageRepository by FakeMessageRepository() {

        val calls = mutableListOf<Triple<String, String?, Int>>()

        override suspend fun getMessages(partnerId: String, before: String?, limit: Int): List<Message> {
            calls += Triple(partnerId, before, limit)
            val callIndex = calls.size - 1
            if (before != null) gateOnCursor?.await()
            if (failOnCall == calls.size) throw RuntimeException("取得失敗")
            return pages.getOrElse(callIndex) { emptyList() }
        }
    }
}
