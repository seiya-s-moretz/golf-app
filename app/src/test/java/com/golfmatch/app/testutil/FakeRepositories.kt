package com.golfmatch.app.testutil

import com.golfmatch.app.domain.model.Area
import com.golfmatch.app.domain.model.AuthSession
import com.golfmatch.app.domain.model.BoardPost
import com.golfmatch.app.domain.model.Conversation
import com.golfmatch.app.domain.model.MatchRequest
import com.golfmatch.app.domain.model.Message
import com.golfmatch.app.domain.model.PhoneOtpVerificationResult
import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.model.RegistrationToken
import com.golfmatch.app.domain.model.Report
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportTargetType
import com.golfmatch.app.domain.model.RoundEvent
import com.golfmatch.app.domain.model.RoundJoinRequest
import com.golfmatch.app.domain.model.User
import com.golfmatch.app.domain.repository.AreaRepository
import com.golfmatch.app.domain.repository.AuthRepository
import com.golfmatch.app.domain.repository.BoardRepository
import com.golfmatch.app.domain.repository.MatchRepository
import com.golfmatch.app.domain.repository.MatchRequestDirection
import com.golfmatch.app.domain.repository.MessageRepository
import com.golfmatch.app.domain.repository.ReportRepository
import com.golfmatch.app.domain.repository.RoundRepository
import com.golfmatch.app.domain.repository.UserRepository
import kotlinx.datetime.Instant

/**
 * UseCase単体テスト用のFakeリポジトリ群。
 *
 * Repository経由のI/O（実際のFirestore/Cloud Functions接続）は本フェーズでは未実装のため、
 * UseCase層が「引数をそのままRepositoryへ委譲し、戻り値をそのまま返す」という契約を守っているかを
 * 検証する目的でのみ使用する。呼び出された引数を記録し、テストで検証できるようにする。
 */

class FakeUserRepository(
    private val recommendedUsers: List<User> = emptyList(),
    private val blockedUsers: List<User> = emptyList()
) : UserRepository {
    var lastBlockedUserId: String? = null
        private set
    var lastUnblockedUserId: String? = null
        private set
    var blockCallCount = 0
        private set
    var unblockCallCount = 0
        private set

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
    ): User = TestFixtures.user(userId = userId, areaId = areaId, purpose = purpose)

    override suspend fun getRecommendedUsers(): List<User> = recommendedUsers

    override suspend fun blockUser(userId: String) {
        lastBlockedUserId = userId
        blockCallCount++
    }

    override suspend fun unblockUser(userId: String) {
        lastUnblockedUserId = userId
        unblockCallCount++
    }

    override suspend fun getBlockedUsers(): List<User> = blockedUsers
}

class FakeRoundRepository(
    private val roundEvents: List<RoundEvent> = emptyList(),
    private val applyJoinResult: RoundJoinRequest = TestFixtures.roundJoinRequest(),
    private val approveResult: RoundJoinRequest = TestFixtures.roundJoinRequest(),
    private val rejectResult: RoundJoinRequest = TestFixtures.roundJoinRequest()
) : RoundRepository {
    var lastAppliedEventId: String? = null
        private set
    var approveCallArgs: Pair<String, String>? = null
        private set
    var rejectCallArgs: Pair<String, String>? = null
        private set

    override suspend fun getRoundEvents(): List<RoundEvent> = roundEvents

    override suspend fun getRoundEvent(eventId: String): RoundEvent = TestFixtures.roundEvent(eventId = eventId)

    override suspend fun createRoundEvent(
        clubName: String,
        datetime: Instant,
        fee: Int,
        capacity: Int
    ): RoundEvent = TestFixtures.roundEvent(capacity = capacity).copy(
        clubName = clubName,
        datetime = datetime,
        fee = fee
    )

    override suspend fun applyJoin(eventId: String): RoundJoinRequest {
        lastAppliedEventId = eventId
        return applyJoinResult
    }

    override suspend fun getJoinRequests(eventId: String): List<RoundJoinRequest> =
        listOf(TestFixtures.roundJoinRequest(eventId = eventId))

    override suspend fun approveJoinRequest(eventId: String, requestId: String): RoundJoinRequest {
        approveCallArgs = eventId to requestId
        return approveResult
    }

    override suspend fun rejectJoinRequest(eventId: String, requestId: String): RoundJoinRequest {
        rejectCallArgs = eventId to requestId
        return rejectResult
    }
}

class FakeBoardRepository(
    private val boardPosts: List<BoardPost> = emptyList()
) : BoardRepository {
    var lastCreatedContent: String? = null
        private set

    override suspend fun getBoardPosts(): List<BoardPost> = boardPosts

    override suspend fun createBoardPost(content: String): BoardPost {
        lastCreatedContent = content
        return TestFixtures.boardPost().copy(content = content)
    }
}

class FakeMatchRepository(
    private val approveResult: MatchRequest = TestFixtures.matchRequest(),
    private val rejectResult: MatchRequest = TestFixtures.matchRequest()
) : MatchRepository {
    var lastSentToUserId: String? = null
        private set
    var lastRequestedDirection: MatchRequestDirection? = null
        private set
    var lastApprovedId: String? = null
        private set
    var lastRejectedId: String? = null
        private set

    override suspend fun sendMatchRequest(toUserId: String): MatchRequest {
        lastSentToUserId = toUserId
        return TestFixtures.matchRequest(toUserId = toUserId)
    }

    override suspend fun getMatchRequests(direction: MatchRequestDirection): List<MatchRequest> {
        lastRequestedDirection = direction
        return listOf(TestFixtures.matchRequest())
    }

    override suspend fun approveMatchRequest(matchRequestId: String): MatchRequest {
        lastApprovedId = matchRequestId
        return approveResult
    }

    override suspend fun rejectMatchRequest(matchRequestId: String): MatchRequest {
        lastRejectedId = matchRequestId
        return rejectResult
    }
}

class FakeMessageRepository(
    private val conversations: List<Conversation> = emptyList(),
    private val messages: List<Message> = emptyList()
) : MessageRepository {
    var lastGetMessagesArgs: Triple<String, String?, Int>? = null
        private set
    var lastSendMessageArgs: Pair<String, String>? = null
        private set
    var lastMarkAsReadPartnerId: String? = null
        private set

    override suspend fun getConversations(): List<Conversation> = conversations

    override suspend fun getMessages(partnerId: String, before: String?, limit: Int): List<Message> {
        lastGetMessagesArgs = Triple(partnerId, before, limit)
        return messages
    }

    override suspend fun sendMessage(partnerId: String, content: String): Message {
        lastSendMessageArgs = partnerId to content
        return TestFixtures.message(userBId = partnerId, senderId = "user-1").copy(content = content)
    }

    override suspend fun markAsRead(partnerId: String) {
        lastMarkAsReadPartnerId = partnerId
    }
}

class FakeReportRepository : ReportRepository {
    var lastSubmitArgs: List<Any?>? = null
        private set

    override suspend fun submitReport(
        targetType: ReportTargetType,
        targetId: String,
        reasonCategory: ReportReasonCategory,
        reasonText: String?
    ): Report {
        lastSubmitArgs = listOf(targetType, targetId, reasonCategory, reasonText)
        return TestFixtures.report(targetType = targetType, reasonCategory = reasonCategory, reasonText = reasonText)
    }
}

class FakeAreaRepository(
    private val areas: List<Area> = listOf(TestFixtures.area())
) : AreaRepository {
    override suspend fun getAreas(): List<Area> = areas
}

class FakeAuthRepository(
    private val verifyResult: PhoneOtpVerificationResult =
        PhoneOtpVerificationResult.NewUser(RegistrationToken("reg-token-1")),
    private val authSession: AuthSession = AuthSession(accessToken = "access-token-1", userId = "user-1")
) : AuthRepository {
    var lastOtpPhoneNumber: String? = null
        private set
    var lastVerifyArgs: Pair<String, String>? = null
        private set
    var lastRegisterArgs: List<Any?>? = null
        private set

    override suspend fun requestPhoneOtp(phoneNumber: String) {
        lastOtpPhoneNumber = phoneNumber
    }

    override suspend fun verifyPhoneOtp(phoneNumber: String, otpCode: String): PhoneOtpVerificationResult {
        lastVerifyArgs = phoneNumber to otpCode
        return verifyResult
    }

    override suspend fun registerUser(
        registrationToken: String,
        name: String,
        gender: String,
        age: Int,
        areaId: String,
        averageScore: Int,
        purpose: Purpose,
        introduction: String
    ): AuthSession {
        lastRegisterArgs = listOf(registrationToken, name, gender, age, areaId, averageScore, purpose, introduction)
        return authSession
    }
}
