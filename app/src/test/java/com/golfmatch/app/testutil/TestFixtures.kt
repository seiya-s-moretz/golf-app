package com.golfmatch.app.testutil

import com.golfmatch.app.domain.model.AccountStatus
import com.golfmatch.app.domain.model.Area
import com.golfmatch.app.domain.model.BoardPost
import com.golfmatch.app.domain.model.Connection
import com.golfmatch.app.domain.model.ConnectionSourceType
import com.golfmatch.app.domain.model.Conversation
import com.golfmatch.app.domain.model.MatchRequest
import com.golfmatch.app.domain.model.MatchRequestStatus
import com.golfmatch.app.domain.model.Message
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.model.Report
import com.golfmatch.app.domain.model.ReportDetail
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.domain.model.ReportTargetUserDetail
import com.golfmatch.app.domain.model.RoundEvent
import com.golfmatch.app.domain.model.RoundJoinRequest
import com.golfmatch.app.domain.model.RoundJoinRequestStatus
import com.golfmatch.app.domain.model.User
import kotlinx.datetime.Instant

/**
 * テスト用のサンプルデータ生成ヘルパー（技術設計書5章のデータモデルに準拠）。
 * 各種テストで共通利用し、テストコードの重複を減らす。
 */
object TestFixtures {

    val fixedInstant: Instant = Instant.parse("2026-08-11T09:00:00Z")

    fun user(
        userId: String = "user-1",
        areaId: String = "area-1",
        purpose: Purpose = Purpose.CASUAL,
        status: AccountStatus = AccountStatus.ACTIVE,
        phoneVerified: Boolean = true
    ): User = User(
        userId = userId,
        name = "山田太郎",
        iconUrl = "https://example.com/icon.png",
        gender = "male",
        age = 30,
        areaId = areaId,
        averageScore = 90,
        purpose = purpose,
        introduction = "よろしくお願いします",
        phoneNumber = "+819012345678",
        phoneVerified = phoneVerified,
        phoneVerifiedAt = if (phoneVerified) fixedInstant else null,
        status = status,
        createdAt = fixedInstant
    )

    fun roundEvent(
        eventId: String = "event-1",
        capacity: Int = 4,
        current: Int = 1,
        createdBy: String = "user-1"
    ): RoundEvent = RoundEvent(
        eventId = eventId,
        clubName = "サンプルゴルフ倶楽部",
        datetime = fixedInstant,
        fee = 8000,
        capacity = capacity,
        current = current,
        createdBy = createdBy,
        createdAt = fixedInstant
    )

    fun roundJoinRequest(
        joinRequestId: String = "join-req-1",
        eventId: String = "event-1",
        userId: String = "user-2",
        status: RoundJoinRequestStatus = RoundJoinRequestStatus.PENDING
    ): RoundJoinRequest = RoundJoinRequest(
        joinRequestId = joinRequestId,
        eventId = eventId,
        userId = userId,
        status = status,
        createdAt = fixedInstant,
        respondedAt = if (status == RoundJoinRequestStatus.PENDING) null else fixedInstant
    )

    fun boardPost(
        postId: String = "post-1",
        userId: String = "user-1"
    ): BoardPost = BoardPost(
        postId = postId,
        userId = userId,
        content = "本日ハーフ48で回れました",
        createdAt = fixedInstant
    )

    fun matchRequest(
        matchRequestId: String = "match-req-1",
        fromUserId: String = "user-1",
        toUserId: String = "user-2",
        status: MatchRequestStatus = MatchRequestStatus.PENDING
    ): MatchRequest = MatchRequest(
        matchRequestId = matchRequestId,
        fromUserId = fromUserId,
        toUserId = toUserId,
        status = status,
        createdAt = fixedInstant,
        respondedAt = if (status == MatchRequestStatus.PENDING) null else fixedInstant
    )

    fun connection(
        connectionId: String = "connection-1",
        userAId: String = "user-1",
        userBId: String = "user-2",
        sourceType: ConnectionSourceType = ConnectionSourceType.MATCH_REQUEST
    ): Connection = Connection(
        connectionId = connectionId,
        userAId = userAId,
        userBId = userBId,
        sourceType = sourceType,
        sourceId = "source-1",
        createdAt = fixedInstant
    )

    fun message(
        messageId: String = "message-1",
        userAId: String = "user-1",
        userBId: String = "user-2",
        senderId: String = "user-1",
        readAt: Instant? = null
    ): Message = Message(
        messageId = messageId,
        userAId = userAId,
        userBId = userBId,
        senderId = senderId,
        content = "今度よろしくお願いします",
        createdAt = fixedInstant,
        readAt = readAt
    )

    fun conversation(
        partner: User = user(userId = "user-2"),
        lastMessage: Message? = message(),
        conversationId: String = "user-1_${partner.userId}"
    ): Conversation = Conversation(
        conversationId = conversationId,
        partner = partner,
        lastMessage = lastMessage,
        unreadCount = 1,
        updatedAt = fixedInstant
    )

    fun report(
        reportId: String = "report-1",
        targetType: ReportTargetType = ReportTargetType.USER,
        reasonCategory: ReportReasonCategory = ReportReasonCategory.SPAM,
        reasonText: String? = null,
        status: ReportStatus = ReportStatus.PENDING
    ): Report = Report(
        reportId = reportId,
        reporterUserId = "user-1",
        targetType = targetType,
        targetId = "target-1",
        reasonCategory = reasonCategory,
        reasonText = reasonText,
        status = status,
        createdAt = fixedInstant,
        handledByUserId = null,
        handledAt = null,
        handlingMemo = null
    )

    /** 通報管理詳細画面（管理者向け）用のサンプルデータ（技術設計書6-9章、ADR-0007） */
    fun reportDetail(
        report: Report = report()
    ): ReportDetail = ReportDetail(
        report = report,
        reporterName = "山田太郎",
        reporterIconUrl = "https://example.com/icon.png",
        targetUser = ReportTargetUserDetail(
            userId = report.targetId,
            name = "鈴木花子",
            iconUrl = "https://example.com/icon2.png",
            gender = "female",
            age = 28,
            introduction = "よろしくお願いします"
        ),
        targetBoardPost = null
    )

    fun area(
        areaId: String = "area-1",
        isActive: Boolean = true,
        displayOrder: Int = 1
    ): Area = Area(
        areaId = areaId,
        prefecture = "埼玉県",
        areaName = "さいたま市",
        displayOrder = displayOrder,
        isActive = isActive,
        createdAt = fixedInstant
    )
}
