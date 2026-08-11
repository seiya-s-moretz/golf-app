package com.golfmatch.app.ui.viewmodel

import com.golfmatch.app.domain.model.MatchRequest
import com.golfmatch.app.domain.model.MatchRequestStatus
import com.golfmatch.app.domain.repository.MatchRepository
import com.golfmatch.app.domain.repository.MatchRequestDirection
import com.golfmatch.app.domain.usecase.GetMatchRequestsUseCase
import com.golfmatch.app.domain.usecase.RespondMatchRequestUseCase
import com.golfmatch.app.testutil.FakeMatchRepository
import com.golfmatch.app.testutil.MainDispatcherRule
import com.golfmatch.app.testutil.TestFixtures
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * 受信マッチング申請一覧画面ViewModelのテスト（技術設計書6-5章）。
 *
 * 承認/却下の分岐、[MatchRequestListUiState.processingRequestId]による多重操作防止ガード、
 * および取得時に`direction=RECEIVED`が指定されることを重点的に検証する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MatchRequestListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadはdirection=RECEIVEDで取得し受信一覧をuiStateに反映する`() = runTest {
        val repo = FakeMatchRepository()
        val viewModel = MatchRequestListViewModel(GetMatchRequestsUseCase(repo), RespondMatchRequestUseCase(repo))

        assertEquals(MatchRequestDirection.RECEIVED, repo.lastRequestedDirection)
        assertEquals(1, viewModel.uiState.value.receivedRequests.size)
    }

    @Test
    fun `respond(approve=true)は承認APIを呼びリストの該当申請のみ更新する`() = runTest {
        val targetId = "match-req-1"
        val repo = FakeMatchRepository(
            approveResult = TestFixtures.matchRequest(matchRequestId = targetId, status = MatchRequestStatus.ACCEPTED)
        )
        val viewModel = MatchRequestListViewModel(GetMatchRequestsUseCase(repo), RespondMatchRequestUseCase(repo))

        viewModel.respond(targetId, approve = true)

        assertEquals(targetId, repo.lastApprovedId)
        assertNull(repo.lastRejectedId)
        assertEquals(
            MatchRequestStatus.ACCEPTED,
            viewModel.uiState.value.receivedRequests.first { it.matchRequestId == targetId }.status
        )
        assertNull(viewModel.uiState.value.processingRequestId)
    }

    @Test
    fun `respond(approve=false)は却下APIを呼ぶ`() = runTest {
        val repo = FakeMatchRepository()
        val viewModel = MatchRequestListViewModel(GetMatchRequestsUseCase(repo), RespondMatchRequestUseCase(repo))
        val targetId = viewModel.uiState.value.receivedRequests.first().matchRequestId

        viewModel.respond(targetId, approve = false)

        assertEquals(targetId, repo.lastRejectedId)
        assertNull(repo.lastApprovedId)
    }

    @Test
    fun `処理中に別の申請へのrespondを呼んでもRepositoryは呼ばれない(多重操作防止)`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var approveCallCount = 0
        val repo = object : MatchRepository by FakeMatchRepository() {
            override suspend fun getMatchRequests(direction: MatchRequestDirection): List<MatchRequest> =
                listOf(
                    TestFixtures.matchRequest(matchRequestId = "match-req-1"),
                    TestFixtures.matchRequest(matchRequestId = "match-req-2")
                )

            override suspend fun approveMatchRequest(matchRequestId: String): MatchRequest {
                approveCallCount++
                gate.await()
                return TestFixtures.matchRequest(matchRequestId = matchRequestId, status = MatchRequestStatus.ACCEPTED)
            }
        }
        val viewModel = MatchRequestListViewModel(GetMatchRequestsUseCase(repo), RespondMatchRequestUseCase(repo))

        viewModel.respond("match-req-1", approve = true)
        assertEquals("match-req-1", viewModel.uiState.value.processingRequestId)

        viewModel.respond("match-req-2", approve = true)

        gate.complete(Unit)

        assertEquals(1, approveCallCount)
        assertNull(viewModel.uiState.value.processingRequestId)
    }
}
