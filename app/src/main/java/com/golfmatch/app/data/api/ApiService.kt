package com.golfmatch.app.data.api

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
import com.golfmatch.app.data.dto.RequestOtpRequestDto
import com.golfmatch.app.data.dto.RoundEventDto
import com.golfmatch.app.data.dto.RoundJoinRequestDto
import com.golfmatch.app.data.dto.SendMessageRequestDto
import com.golfmatch.app.data.dto.SubmitReportRequestDto
import com.golfmatch.app.data.dto.ReportDto
import com.golfmatch.app.data.dto.UpdateReportStatusRequestDto
import com.golfmatch.app.data.dto.UpdateUserRequestDto
import com.golfmatch.app.data.dto.UserDto
import com.golfmatch.app.data.dto.VerifyOtpRequestDto
import com.golfmatch.app.data.dto.VerifyOtpResponseDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * Cloud Functions（HTTPSエンドポイント）呼び出し用API定義（技術設計書 6章）。
 *
 * 認証: `POST /auth/...` 配下の各エンドポイントおよび `GET /areas` を除き、全エンドポイントは
 * `Authorization: Bearer <access_token>` ヘッダーが必要（付与はNetworkModuleのInterceptorが行う）。
 */
interface ApiService {

    // 6-1. 本人確認・認証
    @POST("auth/phone/otp")
    suspend fun requestPhoneOtp(@Body body: RequestOtpRequestDto)

    /**
     * OTP検証と新規/既存ユーザーの判定・認証完了までを1回で行う（技術設計書6-1章、ADR-0006）。
     * 新規/既存で内容の異なる[VerifyOtpResponseDto]を返す。`is_new_user=false`（既存ユーザー）の場合は
     * `session`にアクセストークンとユーザー情報が含まれ認証が完了する（旧`POST /auth/login`はこれに統合され廃止された）。
     * `is_new_user=true`（新規ユーザー）の場合は`registration_token`が含まれ、`POST /users`による本登録に進む。
     */
    @POST("auth/phone/verify")
    suspend fun verifyPhoneOtp(@Body body: VerifyOtpRequestDto): VerifyOtpResponseDto

    @POST("users")
    suspend fun registerUser(@Body body: RegisterUserRequestDto): AuthSessionResponseDto

    // 6-2. エリアマスタ
    @GET("areas")
    suspend fun getAreas(): List<AreaDto>

    // 6-3. ユーザー・プロフィール
    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: String): UserDto

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") userId: String, @Body body: UpdateUserRequestDto): UserDto

    @POST("users/{id}/block")
    suspend fun blockUser(@Path("id") userId: String)

    @DELETE("users/{id}/block")
    suspend fun unblockUser(@Path("id") userId: String)

    @GET("users/me/blocks")
    suspend fun getBlockedUsers(): List<UserDto>

    // 6-4. ラウンド募集
    @GET("round-events")
    suspend fun getRoundEvents(): List<RoundEventDto>

    @GET("round-events/{id}")
    suspend fun getRoundEvent(@Path("id") eventId: String): RoundEventDto

    @POST("round-events")
    suspend fun createRoundEvent(@Body body: CreateRoundEventRequestDto): RoundEventDto

    @POST("round-events/{id}/join-requests")
    suspend fun applyRoundJoin(@Path("id") eventId: String): RoundJoinRequestDto

    @GET("round-events/{id}/join-requests")
    suspend fun getRoundJoinRequests(@Path("id") eventId: String): List<RoundJoinRequestDto>

    @POST("round-events/{id}/join-requests/{requestId}/approve")
    suspend fun approveRoundJoinRequest(
        @Path("id") eventId: String,
        @Path("requestId") requestId: String
    ): RoundJoinRequestDto

    @POST("round-events/{id}/join-requests/{requestId}/reject")
    suspend fun rejectRoundJoinRequest(
        @Path("id") eventId: String,
        @Path("requestId") requestId: String
    ): RoundJoinRequestDto

    // 6-5. おすすめユーザー・マッチング申請
    @GET("users/recommend")
    suspend fun getRecommendedUsers(): List<UserDto>

    @POST("users/{id}/match-requests")
    suspend fun sendMatchRequest(@Path("id") toUserId: String): MatchRequestDto

    @GET("users/me/match-requests")
    suspend fun getMatchRequests(@Query("direction") direction: String): List<MatchRequestDto>

    @POST("match-requests/{id}/approve")
    suspend fun approveMatchRequest(@Path("id") matchRequestId: String): MatchRequestDto

    @POST("match-requests/{id}/reject")
    suspend fun rejectMatchRequest(@Path("id") matchRequestId: String): MatchRequestDto

    // 6-6. 掲示板
    @GET("board")
    suspend fun getBoardPosts(): List<BoardPostDto>

    @POST("board")
    suspend fun createBoardPost(@Body body: CreateBoardPostRequestDto): BoardPostDto

    // 6-7. メッセージ
    @GET("conversations")
    suspend fun getConversations(): List<ConversationDto>

    @GET("conversations/{partnerId}/messages")
    suspend fun getMessages(
        @Path("partnerId") partnerId: String,
        @Query("before") before: String?,
        @Query("limit") limit: Int
    ): List<MessageDto>

    @POST("conversations/{partnerId}/messages")
    suspend fun sendMessage(
        @Path("partnerId") partnerId: String,
        @Body body: SendMessageRequestDto
    ): MessageDto

    @POST("conversations/{partnerId}/read")
    suspend fun markConversationAsRead(@Path("partnerId") partnerId: String)

    // 6-8. 通報・ブロック
    @POST("reports")
    suspend fun submitReport(@Body body: SubmitReportRequestDto): ReportDto

    // 6-9. 通報管理（簡易管理画面、管理者向け。`is_admin=true`のみ許可、falseはサーバー側で403。ADR-0007）
    @GET("admin/reports")
    suspend fun getAdminReports(@Query("status") status: String?): List<ReportAdminSummaryDto>

    @GET("admin/reports/{id}")
    suspend fun getAdminReportDetail(@Path("id") reportId: String): ReportAdminDetailDto

    @PATCH("admin/reports/{id}/status")
    suspend fun updateReportStatus(
        @Path("id") reportId: String,
        @Body body: UpdateReportStatusRequestDto
    ): ReportAdminDetailDto
}
