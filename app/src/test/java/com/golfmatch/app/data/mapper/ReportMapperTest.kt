package com.golfmatch.app.data.mapper

import com.golfmatch.app.data.dto.ReportAdminDetailDto
import com.golfmatch.app.data.dto.ReportAdminReporterDto
import com.golfmatch.app.data.dto.ReportAdminSummaryDto
import com.golfmatch.app.data.dto.ReportAdminTargetBoardPostDto
import com.golfmatch.app.data.dto.ReportAdminTargetDetailDto
import com.golfmatch.app.data.dto.ReportAdminTargetUserDto
import com.golfmatch.app.data.dto.ReportDto
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.model.ReportTargetType
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReportMapperTest {

    private fun dto(
        status: String = "PENDING",
        handledByUserId: String? = null,
        handledAt: String? = null,
        handlingMemo: String? = null,
        reasonCategory: String = "SPAM",
        reasonText: String? = null,
        targetType: String = "USER",
        targetId: String = "user-2"
    ) = ReportDto(
        reportId = "report-1",
        reporterUserId = "user-1",
        targetType = targetType,
        targetId = targetId,
        reasonCategory = reasonCategory,
        reasonText = reasonText,
        status = status,
        createdAt = "2026-08-01T00:00:00Z",
        handledByUserId = handledByUserId,
        handledAt = handledAt,
        handlingMemo = handlingMemo
    )

    @Test
    fun `reason_textがnull(OTHER以外)でも変換できる`() {
        val domain = dto().toDomain()

        assertEquals(ReportTargetType.USER, domain.targetType)
        assertEquals(ReportReasonCategory.SPAM, domain.reasonCategory)
        assertNull(domain.reasonText)
        assertEquals(ReportStatus.PENDING, domain.status)
        assertNull(domain.handledAt)
    }

    @Test
    fun `DATING_SOLICITATION理由・BOARD_POST対象も正しく変換される(PRD 恋愛目的利用禁止の具体化)`() {
        val domain = dto(
            targetType = "BOARD_POST",
            targetId = "post-1",
            reasonCategory = "DATING_SOLICITATION"
        ).toDomain()

        assertEquals(ReportTargetType.BOARD_POST, domain.targetType)
        assertEquals(ReportReasonCategory.DATING_SOLICITATION, domain.reasonCategory)
    }

    @Test
    fun `OTHER理由の場合はreasonTextが値を保持する`() {
        val domain = dto(
            reasonCategory = "OTHER",
            reasonText = "自由記述の理由",
            status = "REVIEWING",
            handledAt = "2026-08-02T00:00:00Z"
        ).toDomain()

        assertEquals("自由記述の理由", domain.reasonText)
        assertEquals(ReportStatus.REVIEWING, domain.status)
    }

    // ---- ReportStatus 4値すべての変換を確認する（ADR-0007: REVIEWED→REVIEWING, ACTION_TAKEN→RESOLVED改称） ----

    @Test
    fun `statusがRESOLVEDの場合も正しく変換される`() {
        val domain = dto(status = "RESOLVED", handledAt = "2026-08-02T00:00:00Z").toDomain()
        assertEquals(ReportStatus.RESOLVED, domain.status)
    }

    @Test
    fun `statusがDISMISSEDの場合も正しく変換される`() {
        val domain = dto(status = "DISMISSED", handledAt = "2026-08-02T00:00:00Z").toDomain()
        assertEquals(ReportStatus.DISMISSED, domain.status)
    }

    // ---- handled_by_user_id / handled_at / handling_memo（ADR-0007追加項目）のnull/非nullの両方 ----

    @Test
    fun `handled系3項目が全てnullの場合(未対応の通報)はnullのまま変換される`() {
        val domain = dto().toDomain()

        assertNull(domain.handledByUserId)
        assertNull(domain.handledAt)
        assertNull(domain.handlingMemo)
    }

    @Test
    fun `handled系3項目が全て非nullの場合(対応済みの通報)は値を保持したまま変換される`() {
        val domain = dto(
            status = "RESOLVED",
            handledByUserId = "admin-1",
            handledAt = "2026-08-03T12:00:00Z",
            handlingMemo = "本人へ警告済み"
        ).toDomain()

        assertEquals("admin-1", domain.handledByUserId)
        assertEquals(Instant.parse("2026-08-03T12:00:00Z"), domain.handledAt)
        assertEquals("本人へ警告済み", domain.handlingMemo)
    }

    // ---- ReportAdminSummaryDto.toDomain()（通報管理一覧、技術設計書6-9章、ADR-0007） ----

    private fun adminSummaryDto(
        status: String = "PENDING",
        handledByUserId: String? = null,
        handledAt: String? = null,
        handlingMemo: String? = null,
        targetSummary: String = "鈴木花子"
    ) = ReportAdminSummaryDto(
        reportId = "report-1",
        reporterUserId = "user-1",
        targetType = "USER",
        targetId = "user-2",
        reasonCategory = "SPAM",
        reasonText = null,
        status = status,
        createdAt = "2026-08-01T00:00:00Z",
        handledByUserId = handledByUserId,
        handledAt = handledAt,
        handlingMemo = handlingMemo,
        reporter = ReportAdminReporterDto(userId = "user-1", name = "山田太郎", iconUrl = "https://example.com/icon.png"),
        targetSummary = targetSummary
    )

    @Test
    fun `ReportAdminSummaryDtoをtoDomainすると一覧表示に必要な項目が全て変換される`() {
        val domain = adminSummaryDto().toDomain()

        assertEquals("report-1", domain.report.reportId)
        assertEquals(ReportStatus.PENDING, domain.report.status)
        assertEquals("山田太郎", domain.reporterName)
        assertEquals("https://example.com/icon.png", domain.reporterIconUrl)
        assertEquals("鈴木花子", domain.targetSummary)
        assertNull(domain.report.handledAt)
    }

    @Test
    fun `ReportAdminSummaryDtoのstatusがREVIEWINGかつhandled系が非nullの場合も正しく変換される`() {
        val domain = adminSummaryDto(
            status = "REVIEWING",
            handledByUserId = "admin-1",
            handledAt = "2026-08-02T00:00:00Z",
            handlingMemo = "確認中"
        ).toDomain()

        assertEquals(ReportStatus.REVIEWING, domain.report.status)
        assertEquals("admin-1", domain.report.handledByUserId)
        assertEquals("確認中", domain.report.handlingMemo)
    }

    // ---- ReportAdminDetailDto.toDomain()（通報管理詳細、技術設計書6-9章、ADR-0007） ----
    // targetType=USERならtargetUserのみ、BOARD_POSTならtargetBoardPostのみが非nullになる（片方のみ）

    @Test
    fun `ReportAdminDetailDtoでtarget_typeがUSERの場合はtargetUserのみ非nullに変換される`() {
        val dto = ReportAdminDetailDto(
            reportId = "report-1",
            reporterUserId = "user-1",
            targetType = "USER",
            targetId = "user-2",
            reasonCategory = "HARASSMENT",
            reasonText = null,
            status = "PENDING",
            createdAt = "2026-08-01T00:00:00Z",
            handledByUserId = null,
            handledAt = null,
            handlingMemo = null,
            reporter = ReportAdminReporterDto(userId = "user-1", name = "山田太郎", iconUrl = "https://example.com/icon.png"),
            targetDetail = ReportAdminTargetDetailDto(
                user = ReportAdminTargetUserDto(
                    userId = "user-2",
                    name = "鈴木花子",
                    iconUrl = "https://example.com/icon2.png",
                    gender = "female",
                    age = 28,
                    introduction = "よろしくお願いします"
                ),
                boardPost = null
            )
        )

        val domain = dto.toDomain()

        requireNotNull(domain.targetUser)
        assertEquals("user-2", domain.targetUser!!.userId)
        assertEquals("鈴木花子", domain.targetUser!!.name)
        assertEquals(28, domain.targetUser!!.age)
        assertNull(domain.targetBoardPost)
    }

    @Test
    fun `ReportAdminDetailDtoでtarget_typeがBOARD_POSTの場合はtargetBoardPostのみ非nullに変換される`() {
        val dto = ReportAdminDetailDto(
            reportId = "report-2",
            reporterUserId = "user-1",
            targetType = "BOARD_POST",
            targetId = "post-1",
            reasonCategory = "INAPPROPRIATE_CONTENT",
            reasonText = null,
            status = "DISMISSED",
            createdAt = "2026-08-01T00:00:00Z",
            handledByUserId = "admin-1",
            handledAt = "2026-08-02T00:00:00Z",
            handlingMemo = "問題なしと判断",
            reporter = ReportAdminReporterDto(userId = "user-1", name = "山田太郎", iconUrl = "https://example.com/icon.png"),
            targetDetail = ReportAdminTargetDetailDto(
                user = null,
                boardPost = ReportAdminTargetBoardPostDto(
                    postId = "post-1",
                    userId = "user-3",
                    authorName = "佐藤次郎",
                    content = "本日ハーフ48で回れました"
                )
            )
        )

        val domain = dto.toDomain()

        assertNull(domain.targetUser)
        requireNotNull(domain.targetBoardPost)
        assertEquals("post-1", domain.targetBoardPost!!.postId)
        assertEquals("user-3", domain.targetBoardPost!!.authorUserId)
        assertEquals("佐藤次郎", domain.targetBoardPost!!.authorName)
        assertEquals(ReportStatus.DISMISSED, domain.report.status)
        assertEquals("admin-1", domain.report.handledByUserId)
        assertEquals("問題なしと判断", domain.report.handlingMemo)
    }
}
