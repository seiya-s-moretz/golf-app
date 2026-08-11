package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.golfmatch.app.data.auth.AuthSessionManager
import com.golfmatch.app.domain.repository.RoundRepository
import com.golfmatch.app.domain.usecase.ApplyRoundJoinUseCase
import com.golfmatch.app.domain.usecase.GetRoundEventUseCase
import com.golfmatch.app.testutil.FakeRoundRepository
import com.golfmatch.app.testutil.MainDispatcherRule
import com.golfmatch.app.testutil.TestFixtures
import com.golfmatch.app.ui.navigation.Route
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * ラウンド詳細画面ViewModelのテスト（技術設計書3-1章・6-4章、ADR-0001）。
 *
 * 主催者判定（[RoundDetailUiState.isOrganizer]、現在ユーザーIDと[RoundEvent.createdBy]の一致）と、
 * 参加申請の多重送信防止（[RoundDetailUiState.isApplying]ガード）を重点的に検証する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoundDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun savedStateHandle(eventId: String = "event-1") =
        SavedStateHandle(mapOf(Route.RoundDetail.ARG_EVENT_ID to eventId))

    @Test
    fun `現在ユーザーIDが募集のcreatedByと一致する場合はisOrganizer=trueになる`() = runTest {
        val roundRepo = FakeRoundRepository(
            roundEventResult = TestFixtures.roundEvent(createdBy = "user-1")
        )
        val authSessionManager = AuthSessionManager().apply { updateSession("token", "user-1") }
        val viewModel = RoundDetailViewModel(
            savedStateHandle(),
            GetRoundEventUseCase(roundRepo),
            ApplyRoundJoinUseCase(roundRepo),
            authSessionManager
        )

        assertTrue(viewModel.uiState.value.isOrganizer)
    }

    @Test
    fun `現在ユーザーIDが募集のcreatedByと異なる場合はisOrganizer=falseになる`() = runTest {
        val roundRepo = FakeRoundRepository(
            roundEventResult = TestFixtures.roundEvent(createdBy = "user-1")
        )
        val authSessionManager = AuthSessionManager().apply { updateSession("token", "user-2") }
        val viewModel = RoundDetailViewModel(
            savedStateHandle(),
            GetRoundEventUseCase(roundRepo),
            ApplyRoundJoinUseCase(roundRepo),
            authSessionManager
        )

        assertFalse(viewModel.uiState.value.isOrganizer)
    }

    @Test
    fun `未ログイン(currentUserId=null)の場合はisOrganizer=falseになる`() = runTest {
        val roundRepo = FakeRoundRepository(
            roundEventResult = TestFixtures.roundEvent(createdBy = "user-1")
        )
        val authSessionManager = AuthSessionManager()
        val viewModel = RoundDetailViewModel(
            savedStateHandle(),
            GetRoundEventUseCase(roundRepo),
            ApplyRoundJoinUseCase(roundRepo),
            authSessionManager
        )

        assertFalse(viewModel.uiState.value.isOrganizer)
    }

    @Test
    fun `load失敗時はerrorMessageが設定されisLoadingはfalseに戻る`() = runTest {
        val roundRepo = object : RoundRepository by FakeRoundRepository() {
            override suspend fun getRoundEvent(eventId: String) = throw IllegalStateException("network error")
        }
        val authSessionManager = AuthSessionManager()
        val viewModel = RoundDetailViewModel(
            savedStateHandle(),
            GetRoundEventUseCase(roundRepo),
            ApplyRoundJoinUseCase(roundRepo),
            authSessionManager
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("network error", state.errorMessage)
        assertNull(state.roundEvent)
    }

    @Test
    fun `applyJoin処理中に再度applyJoinを呼んでもRepositoryは1回しか呼ばれない(多重送信防止)`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var applyCallCount = 0
        val roundRepo = object : RoundRepository by FakeRoundRepository() {
            override suspend fun applyJoin(eventId: String): com.golfmatch.app.domain.model.RoundJoinRequest {
                applyCallCount++
                gate.await()
                return TestFixtures.roundJoinRequest(eventId = eventId)
            }
        }
        val authSessionManager = AuthSessionManager()
        val viewModel = RoundDetailViewModel(
            savedStateHandle(),
            GetRoundEventUseCase(roundRepo),
            ApplyRoundJoinUseCase(roundRepo),
            authSessionManager
        )

        viewModel.applyJoin()
        assertTrue(viewModel.uiState.value.isApplying)

        // 1回目の処理が完了する前に2回目を呼んでもガードで無視される
        viewModel.applyJoin()

        gate.complete(Unit)

        assertEquals(1, applyCallCount)
        assertTrue(viewModel.uiState.value.applySuccess)
        assertFalse(viewModel.uiState.value.isApplying)
    }
}
