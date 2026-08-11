package com.golfmatch.app.domain.usecase

import com.golfmatch.app.testutil.FakeUserRepository
import com.golfmatch.app.testutil.TestFixtures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ユーザー・ブロック関連UseCaseのテスト（技術設計書6-3章、6-5章）。
 *
 * 【推薦ロジックについて】
 * PRD・技術設計書に記載の「スコア差±10:40点／エリア一致:40点／目的一致:20点、60点以上で推薦」
 * というレコメンドロジックは、GetRecommendUsersUseCaseのコメントに明記のとおり
 * 「サーバー側で適用済み」であることが前提の設計であり（技術設計書6-5章 `GET /users/recommend`）、
 * クライアント（本リポジトリ）側にはスコアリングロジックの実装が一切ない
 * （UserRepositoryImpl.getRecommendedUsersはApiServiceの結果をそのまま返すのみ）。
 * サーバーサイド（Cloud Functions）実装は次フェーズのため、本フェーズでは推薦ロジック自体の
 * ユニットテストは実施不可能であり、「テスト対象外」として記録する。
 * ここではUseCaseがRepositoryの結果をそのまま返す（素通しする）ことのみを確認する。
 */
class UserUseCasesTest {

    @Test
    fun `GetRecommendUsersUseCaseはRepositoryの結果をそのまま返す(スコアリングロジックはクライアント未実装)`() = runBlocking {
        val users = listOf(TestFixtures.user(userId = "user-2"), TestFixtures.user(userId = "user-3"))
        val repo = FakeUserRepository(recommendedUsers = users)
        val useCase = GetRecommendUsersUseCase(repo)

        val result = useCase()

        assertEquals(users, result)
    }

    @Test
    fun `BlockUserUseCaseはuserIdをそのままRepositoryへ渡す`() = runBlocking {
        val repo = FakeUserRepository()
        val useCase = BlockUserUseCase(repo)

        useCase("user-2")

        assertEquals("user-2", repo.lastBlockedUserId)
        assertEquals(1, repo.blockCallCount)
    }

    @Test
    fun `UnblockUserUseCaseはuserIdをそのままRepositoryへ渡す`() = runBlocking {
        val repo = FakeUserRepository()
        val useCase = UnblockUserUseCase(repo)

        useCase("user-2")

        assertEquals("user-2", repo.lastUnblockedUserId)
        assertEquals(1, repo.unblockCallCount)
    }

    @Test
    fun `GetBlockedUsersUseCaseはRepositoryの結果をそのまま返す`() = runBlocking {
        val blocked = listOf(TestFixtures.user(userId = "user-9"))
        val repo = FakeUserRepository(blockedUsers = blocked)
        val useCase = GetBlockedUsersUseCase(repo)

        assertEquals(blocked, useCase())
    }
}
