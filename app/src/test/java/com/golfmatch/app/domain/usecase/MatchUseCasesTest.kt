package com.golfmatch.app.domain.usecase

import com.golfmatch.app.testutil.FakeMatchRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * マッチング申請関連UseCaseのテスト（技術設計書6-5章）。
 *
 * (from_user_id, to_user_id) の組み合わせでPENDING状態は1件までという重複防止制約は
 * サーバー側バリデーション（技術設計書5-2章の制約）であり、クライアント側UseCase/Repositoryには
 * 実装されていない（Repository経由のI/Oが必要なためテスト対象外）。
 */
class MatchUseCasesTest {

    @Test
    fun `SendMatchRequestUseCaseはtoUserIdをそのままRepositoryへ渡す`() = runBlocking {
        val repo = FakeMatchRepository()
        val useCase = SendMatchRequestUseCase(repo)

        val result = useCase("user-2")

        assertEquals("user-2", repo.lastSentToUserId)
        assertEquals("user-2", result.toUserId)
    }

    @Test
    fun `RespondMatchRequestUseCaseはapprove=trueのときapproveMatchRequestを呼ぶ`() = runBlocking {
        val repo = FakeMatchRepository()
        val useCase = RespondMatchRequestUseCase(repo)

        useCase("match-req-1", approve = true)

        assertEquals("match-req-1", repo.lastApprovedId)
        assertNull(repo.lastRejectedId)
    }

    @Test
    fun `RespondMatchRequestUseCaseはapprove=falseのときrejectMatchRequestを呼ぶ`() = runBlocking {
        val repo = FakeMatchRepository()
        val useCase = RespondMatchRequestUseCase(repo)

        useCase("match-req-1", approve = false)

        assertEquals("match-req-1", repo.lastRejectedId)
        assertNull(repo.lastApprovedId)
    }
}
