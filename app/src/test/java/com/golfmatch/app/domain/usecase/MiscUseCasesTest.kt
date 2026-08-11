package com.golfmatch.app.domain.usecase

import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.testutil.FakeAreaRepository
import com.golfmatch.app.testutil.FakeBoardRepository
import com.golfmatch.app.testutil.FakeReportRepository
import com.golfmatch.app.testutil.TestFixtures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * エリアマスタ・掲示板・通報関連UseCaseのテスト（技術設計書6-2章、6-6章、6-8章）。
 */
class MiscUseCasesTest {

    @Test
    fun `GetAreasUseCaseはRepositoryの結果をそのまま返す(is_active・表示順のフィルタはサーバー側の責務)`() = runBlocking {
        val areas = listOf(TestFixtures.area(areaId = "area-1", displayOrder = 1), TestFixtures.area(areaId = "area-2", displayOrder = 2))
        val repo = FakeAreaRepository(areas = areas)
        val useCase = GetAreasUseCase(repo)

        assertEquals(areas, useCase())
    }

    @Test
    fun `PostBoardMessageUseCaseはcontentをそのままRepositoryへ渡す(テキストのみ、PRD 6章)`() = runBlocking {
        val repo = FakeBoardRepository()
        val useCase = PostBoardMessageUseCase(repo)

        val result = useCase("本日ハーフ48で回れました")

        assertEquals("本日ハーフ48で回れました", repo.lastCreatedContent)
        assertEquals("本日ハーフ48で回れました", result.content)
    }

    @Test
    fun `SubmitReportUseCaseは全パラメータをそのままRepositoryへ渡す(技術設計書6-8章)`() = runBlocking {
        val repo = FakeReportRepository()
        val useCase = SubmitReportUseCase(repo)

        useCase(
            targetType = ReportTargetType.USER,
            targetId = "user-2",
            reasonCategory = ReportReasonCategory.OTHER,
            reasonText = "自由記述"
        )

        assertEquals(
            listOf(ReportTargetType.USER, "user-2", ReportReasonCategory.OTHER, "自由記述"),
            repo.lastSubmitArgs
        )
    }
}
