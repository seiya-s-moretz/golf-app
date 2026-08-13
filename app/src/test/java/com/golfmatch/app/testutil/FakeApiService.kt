package com.golfmatch.app.testutil

import com.golfmatch.app.data.api.ApiService
import com.golfmatch.app.data.dto.AreaDto
import com.golfmatch.app.data.dto.AuthSessionResponseDto
import com.golfmatch.app.data.dto.BoardPostDto
import com.golfmatch.app.data.dto.ConversationDto
import com.golfmatch.app.data.dto.CreateBoardPostRequestDto
import com.golfmatch.app.data.dto.CreateRoundEventRequestDto
import com.golfmatch.app.data.dto.MatchRequestDto
import com.golfmatch.app.data.dto.MessageDto
import com.golfmatch.app.data.dto.RegisterUserRequestDto
import com.golfmatch.app.data.dto.ReportAdminDetailDto
import com.golfmatch.app.data.dto.ReportAdminSummaryDto
import com.golfmatch.app.data.dto.ReportDto
import com.golfmatch.app.data.dto.RequestOtpRequestDto
import com.golfmatch.app.data.dto.RoundEventDto
import com.golfmatch.app.data.dto.RoundJoinRequestDto
import com.golfmatch.app.data.dto.SendMessageRequestDto
import com.golfmatch.app.data.dto.SubmitReportRequestDto
import com.golfmatch.app.data.dto.UpdateReportStatusRequestDto
import com.golfmatch.app.data.dto.UpdateUserRequestDto
import com.golfmatch.app.data.dto.UserDto
import com.golfmatch.app.data.dto.VerifyOtpRequestDto
import com.golfmatch.app.data.dto.VerifyOtpResponseDto

/**
 * `data/repository/impl`配下のRepository実装をテストするための[ApiService]スタブ
 * （`docs/test-plan.md` 4-3章で「ApiServiceのFake/Mock整備」として挙げられていたもの）。
 *
 * 全メソッドは既定で「呼ばれたら失敗」とし、テストごとにKotlinの委譲
 * （`object : ApiService by FakeApiService() { override ... }`）で必要なメソッドだけを差し替える。
 * こうすることで「そのテストが本当に呼ぶはずのエンドポイント」以外が呼ばれた場合に検知できる。
 *
 * 本プロジェクトはモックライブラリを導入せずFakeを手書きする方針のため（`testutil/FakeRepositories.kt`と同様）、
 * [ApiService]にメソッドを追加した場合は本クラスにも追加する必要がある。
 */
open class FakeApiService : ApiService {

    private fun notStubbed(name: String): Nothing =
        throw NotImplementedError("FakeApiService.$name はこのテストでスタブされていません")

    override suspend fun requestPhoneOtp(body: RequestOtpRequestDto): Unit = notStubbed("requestPhoneOtp")

    override suspend fun verifyPhoneOtp(body: VerifyOtpRequestDto): VerifyOtpResponseDto =
        notStubbed("verifyPhoneOtp")

    override suspend fun registerUser(body: RegisterUserRequestDto): AuthSessionResponseDto =
        notStubbed("registerUser")

    override suspend fun getAreas(): List<AreaDto> = notStubbed("getAreas")

    override suspend fun getUser(userId: String): UserDto = notStubbed("getUser")

    override suspend fun updateUser(userId: String, body: UpdateUserRequestDto): UserDto = notStubbed("updateUser")

    override suspend fun blockUser(userId: String): Unit = notStubbed("blockUser")

    override suspend fun unblockUser(userId: String): Unit = notStubbed("unblockUser")

    override suspend fun getBlockedUsers(): List<UserDto> = notStubbed("getBlockedUsers")

    override suspend fun getRoundEvents(): List<RoundEventDto> = notStubbed("getRoundEvents")

    override suspend fun getRoundEvent(eventId: String): RoundEventDto = notStubbed("getRoundEvent")

    override suspend fun createRoundEvent(body: CreateRoundEventRequestDto): RoundEventDto =
        notStubbed("createRoundEvent")

    override suspend fun applyRoundJoin(eventId: String): RoundJoinRequestDto = notStubbed("applyRoundJoin")

    override suspend fun getRoundJoinRequests(eventId: String): List<RoundJoinRequestDto> =
        notStubbed("getRoundJoinRequests")

    override suspend fun approveRoundJoinRequest(eventId: String, requestId: String): RoundJoinRequestDto =
        notStubbed("approveRoundJoinRequest")

    override suspend fun rejectRoundJoinRequest(eventId: String, requestId: String): RoundJoinRequestDto =
        notStubbed("rejectRoundJoinRequest")

    override suspend fun getRecommendedUsers(): List<UserDto> = notStubbed("getRecommendedUsers")

    override suspend fun sendMatchRequest(toUserId: String): MatchRequestDto = notStubbed("sendMatchRequest")

    override suspend fun getMatchRequests(direction: String): List<MatchRequestDto> = notStubbed("getMatchRequests")

    override suspend fun approveMatchRequest(matchRequestId: String): MatchRequestDto =
        notStubbed("approveMatchRequest")

    override suspend fun rejectMatchRequest(matchRequestId: String): MatchRequestDto = notStubbed("rejectMatchRequest")

    override suspend fun getBoardPosts(before: String?, beforeId: String?, limit: Int): List<BoardPostDto> =
        notStubbed("getBoardPosts")

    override suspend fun createBoardPost(body: CreateBoardPostRequestDto): BoardPostDto =
        notStubbed("createBoardPost")

    override suspend fun getConversations(): List<ConversationDto> = notStubbed("getConversations")

    override suspend fun getMessages(
        partnerId: String,
        before: String?,
        beforeId: String?,
        limit: Int
    ): List<MessageDto> = notStubbed("getMessages")

    override suspend fun sendMessage(partnerId: String, body: SendMessageRequestDto): MessageDto =
        notStubbed("sendMessage")

    override suspend fun markConversationAsRead(partnerId: String): Unit = notStubbed("markConversationAsRead")

    override suspend fun submitReport(body: SubmitReportRequestDto): ReportDto = notStubbed("submitReport")

    override suspend fun getAdminReports(
        status: String?,
        before: String?,
        beforeId: String?,
        limit: Int
    ): List<ReportAdminSummaryDto> = notStubbed("getAdminReports")

    override suspend fun getAdminReportDetail(reportId: String): ReportAdminDetailDto =
        notStubbed("getAdminReportDetail")

    override suspend fun updateReportStatus(
        reportId: String,
        body: UpdateReportStatusRequestDto
    ): ReportAdminDetailDto = notStubbed("updateReportStatus")
}
