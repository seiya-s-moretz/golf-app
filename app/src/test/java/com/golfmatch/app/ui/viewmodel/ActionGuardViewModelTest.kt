package com.golfmatch.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.golfmatch.app.data.auth.AuthSessionManager
import com.golfmatch.app.domain.model.BoardPost
import com.golfmatch.app.domain.model.MatchRequest
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.model.Report
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.domain.model.RoundEvent
import com.golfmatch.app.domain.model.User
import com.golfmatch.app.domain.repository.BoardRepository
import com.golfmatch.app.domain.repository.MatchRepository
import com.golfmatch.app.domain.repository.ReportRepository
import com.golfmatch.app.domain.repository.RoundRepository
import com.golfmatch.app.domain.repository.UserRepository
import com.golfmatch.app.domain.usecase.BlockUserUseCase
import com.golfmatch.app.domain.usecase.CreateRoundEventUseCase
import com.golfmatch.app.domain.usecase.GetAreasUseCase
import com.golfmatch.app.domain.usecase.GetRecommendUsersUseCase
import com.golfmatch.app.domain.usecase.GetUserUseCase
import com.golfmatch.app.domain.usecase.PostBoardMessageUseCase
import com.golfmatch.app.domain.usecase.SendMatchRequestUseCase
import com.golfmatch.app.domain.usecase.SubmitReportUseCase
import com.golfmatch.app.domain.usecase.UpdateUserProfileUseCase
import com.golfmatch.app.testutil.FakeAreaRepository
import com.golfmatch.app.testutil.FakeBoardRepository
import com.golfmatch.app.testutil.FakeMatchRepository
import com.golfmatch.app.testutil.FakeReportRepository
import com.golfmatch.app.testutil.FakeRoundRepository
import com.golfmatch.app.testutil.FakeUserRepository
import com.golfmatch.app.testutil.MainDispatcherRule
import com.golfmatch.app.testutil.TestFixtures
import com.golfmatch.app.ui.navigation.Route
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * 送信系アクションの多重操作防止ガードのテスト（`docs/test-plan.md` 4-4章・4-3章・4-9章）。
 *
 * 本プロジェクトは「処理中は再実行しない」ガードを全画面で持つ方針だが、実際には画面ごとに
 * 有無がばらついていた。特に**成功直後**は`isSubmitting`が下りる一方で画面遷移が完了するまで
 * ボタンが操作可能なままであり、ここを塞がないと重複作成・重複投稿が起きる。
 * 画面ごとにテストファイルを分けると同じ検証の写経になるため、1ファイルにまとめて回帰検知する。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActionGuardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `ラウンド募集作成は成功後に再度submitしても二重作成しない`() = runTest {
        var callCount = 0
        val repo = object : RoundRepository by FakeRoundRepository() {
            override suspend fun createRoundEvent(
                clubName: String,
                datetime: Instant,
                fee: Int,
                capacity: Int
            ): RoundEvent {
                callCount++
                return TestFixtures.roundEvent()
            }
        }
        val viewModel = CreateRoundViewModel(CreateRoundEventUseCase(repo))

        viewModel.onClubNameChange("テストGC")
        viewModel.onDateTimeChange("2026-09-01T09:00")
        viewModel.onFeeChange("10000")
        viewModel.onCapacityChange("4")
        viewModel.submit()
        viewModel.submit()

        assertEquals(1, callCount)
        assertEquals(true, viewModel.uiState.value.submitSuccess)
    }

    @Test
    fun `掲示板投稿は成功後に再度submitしても二重投稿しない`() = runTest {
        var callCount = 0
        val repo = object : BoardRepository by FakeBoardRepository() {
            override suspend fun createBoardPost(content: String): BoardPost {
                callCount++
                return TestFixtures.boardPost()
            }
        }
        val viewModel = CreateBoardPostViewModel(PostBoardMessageUseCase(repo))
        // 投稿作成のガードは掲示板一覧のブロック導線とは独立（CreateBoardPostViewModelは投稿のみを担う）

        viewModel.onContentChange("一緒に回りませんか")
        viewModel.submit()
        viewModel.submit()

        assertEquals(1, callCount)
    }

    @Test
    fun `通報は成功後に再度submitしても二重送信しない`() = runTest {
        var callCount = 0
        val repo = object : ReportRepository by FakeReportRepository() {
            override suspend fun submitReport(
                targetType: ReportTargetType,
                targetId: String,
                reasonCategory: ReportReasonCategory,
                reasonText: String?
            ): Report {
                callCount++
                return TestFixtures.report()
            }
        }
        val viewModel = ReportViewModel(
            SavedStateHandle(
                mapOf(
                    Route.Report.ARG_TARGET_TYPE to ReportTargetType.USER.name,
                    Route.Report.ARG_TARGET_ID to "user-2"
                )
            ),
            SubmitReportUseCase(repo)
        )

        viewModel.onReasonCategorySelected(ReportReasonCategory.SPAM)
        viewModel.submit()
        viewModel.submit()

        assertEquals(1, callCount)
    }

    @Test
    fun `マッチング申請は送信完了を待たずに再タップしても二重送信しない`() = runTest {
        val gate = CompletableDeferred<Unit>()
        var callCount = 0
        val repo = object : MatchRepository by FakeMatchRepository() {
            override suspend fun sendMatchRequest(toUserId: String): MatchRequest {
                callCount++
                gate.await()
                return TestFixtures.matchRequest()
            }
        }
        val viewModel = RecommendViewModel(
            GetRecommendUsersUseCase(FakeUserRepository()),
            GetAreasUseCase(FakeAreaRepository()),
            SendMatchRequestUseCase(repo),
            BlockUserUseCase(FakeUserRepository())
        )
        val target = TestFixtures.user(userId = "user-2")

        // 送信中は`sentRequestUserIds`にまだ入らないため、それだけを見るガードでは弾けない
        viewModel.sendMatchRequest(target)
        viewModel.sendMatchRequest(target)
        gate.complete(Unit)

        assertEquals(1, callCount)
        assertEquals(setOf("user-2"), viewModel.uiState.value.sentRequestUserIds)
        assertEquals(emptySet<String>(), viewModel.uiState.value.sendingRequestUserIds)
    }

    @Test
    fun `プロフィール保存は保存中に再度saveを呼んでも二重更新しない`() = runTest {
        // プロフィール更新は冪等なため成功後の再保存は許容している（編集し直して保存できる必要がある）。
        // ここで塞ぐのは「レスポンス待ちの最中の再タップ」なのでゲートで保留させて検証する
        val gate = CompletableDeferred<Unit>()
        var callCount = 0
        val sessionManager = AuthSessionManager().apply { updateSession("access-token-1", "user-1") }
        val repo = object : UserRepository by FakeUserRepository() {
            override suspend fun getUser(userId: String): User = TestFixtures.user(userId = userId)

            override suspend fun updateUser(
                userId: String,
                name: String,
                gender: String,
                age: Int,
                areaId: String,
                averageScore: Int,
                purpose: Purpose,
                introduction: String
            ): User {
                callCount++
                gate.await()
                return TestFixtures.user(userId = userId)
            }
        }
        val viewModel = MyPageViewModel(
            GetUserUseCase(repo),
            UpdateUserProfileUseCase(repo),
            GetAreasUseCase(FakeAreaRepository()),
            sessionManager
        )

        viewModel.save()
        viewModel.save()
        gate.complete(Unit)

        assertEquals(1, callCount)
        assertEquals(true, viewModel.uiState.value.saveSuccess)
    }
}
