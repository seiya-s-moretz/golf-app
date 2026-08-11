package com.golfmatch.app.domain.usecase

import com.golfmatch.app.testutil.FakeMessageRepository
import com.golfmatch.app.testutil.TestFixtures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * メッセージ機能関連UseCaseのテスト（技術設計書6-7章、ADR-0004）。
 *
 * Connection有無・ブロック関係による送信可否のアクセス制御はサーバー側の責務であり、
 * 現在のMessageRepositoryImplはApiServiceへの薄い委譲のみ（クライアント側にアクセス制御ロジックはない）。
 * そのためここではUseCase→Repositoryへの委譲が正しく行われているかのみを検証する。
 */
class MessageUseCasesTest {

    @Test
    fun `GetConversationsUseCaseはRepositoryの結果をそのまま返す`() = runBlocking {
        val conversations = listOf(TestFixtures.conversation())
        val repo = FakeMessageRepository(conversations = conversations)
        val useCase = GetConversationsUseCase(repo)

        val result = useCase()

        assertEquals(conversations, result)
    }

    @Test
    fun `GetMessagesUseCaseはpartnerId・before・limitをそのまま渡す`() = runBlocking {
        val repo = FakeMessageRepository()
        val useCase = GetMessagesUseCase(repo)

        useCase("user-2", before = "cursor-1", limit = 20)

        assertEquals(Triple("user-2", "cursor-1", 20), repo.lastGetMessagesArgs)
    }

    @Test
    fun `GetMessagesUseCaseはbefore省略時にnull・limitはデフォルト50を渡す`() = runBlocking {
        val repo = FakeMessageRepository()
        val useCase = GetMessagesUseCase(repo)

        useCase("user-2")

        assertEquals(Triple("user-2", null, 50), repo.lastGetMessagesArgs)
    }

    @Test
    fun `SendMessageUseCaseはpartnerIdとcontentをそのまま渡す`() = runBlocking {
        val repo = FakeMessageRepository()
        val useCase = SendMessageUseCase(repo)

        val result = useCase("user-2", "よろしくお願いします")

        assertEquals("user-2" to "よろしくお願いします", repo.lastSendMessageArgs)
        assertEquals("よろしくお願いします", result.content)
    }
}
