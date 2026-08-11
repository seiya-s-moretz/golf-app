package com.golfmatch.app.domain.model

import com.golfmatch.app.testutil.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 技術設計書5章のデータモデル定義とdomain/modelのEntity定義の照合テスト。
 *
 * Kotlinは静的型付けのため型・必須/任意（null許容）は基本的にコンパイル時に保証されるが、
 * ここでは特に
 *  - enum値の網羅性（サーバーとの表現齟齬がないか）
 *  - nullableフィールド（?付き）が実際にnullを許容し、応答未設定時の状態を表現できるか
 * を明示的にテストし、設計書との不一致を検出できるようにする。
 */
class EntityDataModelTest {

    // ---- RoundJoinRequestStatus: 技術設計書5-2章 PENDING / APPROVED / REJECTED ----
    @Test
    fun `RoundJoinRequestStatus は設計書どおり3値`() {
        assertEquals(
            setOf("PENDING", "APPROVED", "REJECTED"),
            RoundJoinRequestStatus.entries.map { it.name }.toSet()
        )
    }

    // ---- MatchRequestStatus: 技術設計書5-2章 PENDING / ACCEPTED / REJECTED ----
    @Test
    fun `MatchRequestStatus は設計書どおり3値`() {
        assertEquals(
            setOf("PENDING", "ACCEPTED", "REJECTED"),
            MatchRequestStatus.entries.map { it.name }.toSet()
        )
    }

    // ---- ConnectionSourceType: 技術設計書5-2章 MATCH_REQUEST / ROUND_JOIN ----
    @Test
    fun `ConnectionSourceType は設計書どおり2値`() {
        assertEquals(
            setOf("MATCH_REQUEST", "ROUND_JOIN"),
            ConnectionSourceType.entries.map { it.name }.toSet()
        )
    }

    // ---- ReportTargetType: 技術設計書5-2章 USER / BOARD_POST ----
    @Test
    fun `ReportTargetType は設計書どおり2値`() {
        assertEquals(
            setOf("USER", "BOARD_POST"),
            ReportTargetType.entries.map { it.name }.toSet()
        )
    }

    // ---- ReportReasonCategory: 技術設計書5-2章 SPAM/DATING_SOLICITATION/HARASSMENT/INAPPROPRIATE_CONTENT/OTHER ----
    @Test
    fun `ReportReasonCategory は設計書どおり5値(DATING_SOLICITATIONを含む)`() {
        assertEquals(
            setOf("SPAM", "DATING_SOLICITATION", "HARASSMENT", "INAPPROPRIATE_CONTENT", "OTHER"),
            ReportReasonCategory.entries.map { it.name }.toSet()
        )
    }

    // ---- ReportStatus: 技術設計書5-2章 PENDING/REVIEWED/ACTION_TAKEN/DISMISSED ----
    @Test
    fun `ReportStatus は設計書どおり4値`() {
        assertEquals(
            setOf("PENDING", "REVIEWED", "ACTION_TAKEN", "DISMISSED"),
            ReportStatus.entries.map { it.name }.toSet()
        )
    }

    // ---- AccountStatus: 技術設計書5-1章 ACTIVE/SUSPENDED ----
    @Test
    fun `AccountStatus は設計書どおり2値`() {
        assertEquals(
            setOf("ACTIVE", "SUSPENDED"),
            AccountStatus.entries.map { it.name }.toSet()
        )
    }

    // ---- PhoneVerificationStatus: 技術設計書5-2章 PENDING/VERIFIED/EXPIRED/FAILED ----
    @Test
    fun `PhoneVerificationStatus は設計書どおり4値`() {
        assertEquals(
            setOf("PENDING", "VERIFIED", "EXPIRED", "FAILED"),
            PhoneVerificationStatus.entries.map { it.name }.toSet()
        )
    }

    // ---- Nullable フィールドの確認 ----

    @Test
    fun `RoundJoinRequest respondedAt はPENDING時にnullを許容する(未回答を表現できる)`() {
        val request = TestFixtures.roundJoinRequest(status = RoundJoinRequestStatus.PENDING)
        assertNull(request.respondedAt)
    }

    @Test
    fun `MatchRequest respondedAt はPENDING時にnullを許容する`() {
        val request = TestFixtures.matchRequest(status = MatchRequestStatus.PENDING)
        assertNull(request.respondedAt)
    }

    @Test
    fun `Message readAt は未読時にnullを許容する(技術設計書5-2章 Message)`() {
        val message = TestFixtures.message(readAt = null)
        assertNull(message.readAt)
    }

    @Test
    fun `Report reasonText はnullを許容する(OTHER以外では省略可、技術設計書5-2章)`() {
        val report = TestFixtures.report(reasonText = null)
        assertNull(report.reasonText)
    }

    @Test
    fun `User phoneVerifiedAt は未確認時にnullを許容する(技術設計書5-1章)`() {
        val user = TestFixtures.user(phoneVerified = false)
        assertNull(user.phoneVerifiedAt)
        assertEquals(false, user.phoneVerified)
    }

    @Test
    fun `Area isActive がfalseでも構造上は保持できる(廃止ではなく非表示、ADR-0002)`() {
        val inactiveArea = TestFixtures.area(isActive = false)
        assertEquals(false, inactiveArea.isActive)
    }
}
