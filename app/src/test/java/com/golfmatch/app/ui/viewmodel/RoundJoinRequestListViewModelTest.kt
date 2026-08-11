package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.golfmatch.app.domain.model.RoundJoinRequest
import com.golfmatch.app.domain.model.RoundJoinRequestStatus
import com.golfmatch.app.domain.repository.RoundRepository
import com.golfmatch.app.domain.usecase.ApproveRoundJoinUseCase
import com.golfmatch.app.domain.usecase.GetRoundJoinRequestsUseCase
import com.golfmatch.app.testutil.FakeRoundRepository
import com.golfmatch.app.testutil.MainDispatcherRule
import com.golfmatch.app.testutil.TestFixtures
import com.golfmatch.app.ui.navigation.Route
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * ラウンド参加申請一覧画面（主催者向け）ViewModelのテスト（技術設計書6-4章、ADR-0001）。
 *
 * 承認/却下の分岐、および[RoundJoinRequestListUiState.processingRequestId]による
 * 多重操作防止ガードを重点的に検証する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoundJoinRequestListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun savedStateHandle(eventId: String = "event-1") =
        SavedStateHandle(mapOf(Route.RoundJoinRequestList.ARG_EVENT_ID to eventId))

    @Test
    fun `loadで取得した申請一覧がuiStateに反映される`() = runTest {
        val repo = FakeRoundRepository()
        val viewModel = RoundJoinRequestListViewModel(
            savedStateHandle(),
            GetRoundJoinRequestsUseCase(repo),
            ApproveRoundJoinUseCase(repo)
        )

        assertEquals(1, viewModel.uiState.value.requests.size)
        assertEquals("event-1", viewModel.uiState.value.eventId)
    }

    @Test
    fun `respond(approve=true)は承認APIを呼びリストの該当申請のみ更新する`() = runTest {
        val targetId = "join-req-1"
        val repo = FakeRoundRepository(
            approveResult = TestFixtures.roundJoinRequest(joinRequestId = targetId, status = RoundJoinRequestStatus.APPROVED)
        )
        val viewModel = RoundJoinRequestListViewModel(
            savedStateHandle(),
            GetRoundJoinRequestsUseCase(repo),
            ApproveRoundJoinUseCase(repo)
        )
        assertEquals(targetId, viewModel.uiState.value.requests.first().joinRequestId)

        viewModel.respond(targetId, approve = true)

        assertEquals("event-1" to targetId, repo.approveCallArgs)
        assertNull(repo.rejectCallArgs)
        assertEquals(RoundJoinRequestStatus.APPROVED, viewModel.uiState.value.requests.first { it.joinRequestId == targetId }.status)
        assertNull(viewModel.uiState.value.processingRequestId)
    }

    @Test
    fun `respond(approve=false)は却下APIを呼ぶ`() = runTest {
        val repo = FakeRoundRepository()
        val viewModel = RoundJoinRequestListViewModel(
            savedStateHandle(),
            GetRoundJoinRequestsUseCase(repo),
            ApproveRoundJoinUseCase(repo)
        )
        val targetId = viewModel.uiState.value.requests.first().joinRequestId

        viewModel.respond(targetId, approve = false)

        assertEquals("event-1" to targetId, repo.rejectCallArgs)
        assertNull(repo.approveCallArgs)
    }

    @Test
    fun `処理中に別の申請へのrespondを呼んでもRepositoryは呼ばれない(多重操作防止)`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var approveCallCount = 0
        val repo = object : RoundRepository by FakeRoundRepository() {
            override suspend fun getJoinRequests(eventId: String): List<RoundJoinRequest> =
                listOf(
                    TestFixtures.roundJoinRequest(joinRequestId = "join-req-1", eventId = eventId),
                    TestFixtures.roundJoinRequest(joinRequestId = "join-req-2", eventId = eventId)
                )

            override suspend fun approveJoinRequest(eventId: String, requestId: String): RoundJoinRequest {
                approveCallCount++
                gate.await()
                return TestFixtures.roundJoinRequest(joinRequestId = requestId, eventId = eventId, status = RoundJoinRequestStatus.APPROVED)
            }
        }
        val viewModel = RoundJoinRequestListViewModel(
            savedStateHandle(),
            GetRoundJoinRequestsUseCase(repo),
            ApproveRoundJoinUseCase(repo)
        )

        viewModel.respond("join-req-1", approve = true)
        assertEquals("join-req-1", viewModel.uiState.value.processingRequestId)

        // 1件目の処理が完了する前に2件目への操作を試みても無視される
        viewModel.respond("join-req-2", approve = true)

        gate.complete(Unit)

        assertEquals(1, approveCallCount)
        assertTrue(viewModel.uiState.value.processingRequestId == null)
    }
}
