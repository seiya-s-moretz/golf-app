package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.RoundJoinRequestStatus
import com.golfmatch.app.testutil.FakeRoundRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ラウンド募集・参加申請フロー関連UseCaseのテスト（技術設計書6-4章、ADR-0001）。
 *
 * 現時点のRepository実装（RoundRepositoryImpl）はApiServiceを呼び出す薄い実装であり、
 * 「capacity > current」等のビジネスルール自体はサーバー側（Cloud Functions、未実装）が担う。
 * そのためクライアント側UseCaseで検証できるのは、
 *  - UseCaseがRepositoryへ引数を正しく委譲しているか
 *  - approve/rejectの分岐が正しいメソッドを呼び分けているか（ApproveRoundJoinUseCase自体のロジック）
 * の2点である。capacity超過チェック等はサーバー未実装のためテスト対象外として明記する。
 */
class RoundUseCasesTest {

    @Test
    fun `ApplyRoundJoinUseCaseはeventIdをそのままRepositoryへ渡す`() = runBlocking {
        val repo = FakeRoundRepository()
        val useCase = ApplyRoundJoinUseCase(repo)

        val result = useCase("event-1")

        assertEquals("event-1", repo.lastAppliedEventId)
        assertEquals(RoundJoinRequestStatus.PENDING, result.status)
    }

    @Test
    fun `ApproveRoundJoinUseCaseはapprove=trueのときapproveJoinRequestを呼ぶ`() = runBlocking {
        val repo = FakeRoundRepository()
        val useCase = ApproveRoundJoinUseCase(repo)

        useCase("event-1", "join-req-1", approve = true)

        assertEquals("event-1" to "join-req-1", repo.approveCallArgs)
        assertEquals(null, repo.rejectCallArgs)
    }

    @Test
    fun `ApproveRoundJoinUseCaseはapprove=falseのときrejectJoinRequestを呼ぶ`() = runBlocking {
        val repo = FakeRoundRepository()
        val useCase = ApproveRoundJoinUseCase(repo)

        useCase("event-1", "join-req-1", approve = false)

        assertEquals("event-1" to "join-req-1", repo.rejectCallArgs)
        assertEquals(null, repo.approveCallArgs)
    }

    @Test
    fun `CreateRoundEventUseCaseは入力パラメータをそのままRepositoryへ渡す`() = runBlocking {
        val repo = FakeRoundRepository()
        val useCase = CreateRoundEventUseCase(repo)
        val datetime = Instant.parse("2026-09-01T00:00:00Z")

        val result = useCase("サンプル倶楽部", datetime, 8000, 4)

        assertEquals("サンプル倶楽部", result.clubName)
        assertEquals(datetime, result.datetime)
        assertEquals(8000, result.fee)
        assertEquals(4, result.capacity)
    }
}
