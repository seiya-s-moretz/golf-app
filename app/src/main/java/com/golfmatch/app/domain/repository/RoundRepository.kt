package com.golfmatch.app.domain.repository

import com.golfmatch.app.domain.model.RoundEvent
import com.golfmatch.app.domain.model.RoundJoinRequest
import kotlinx.datetime.Instant

/**
 * ラウンド募集・参加申請フローのリポジトリ（技術設計書 6-4章、ADR-0001）
 */
interface RoundRepository {
    /** ブロック関係にある作成者の募集はサーバー側で除外済み（技術設計書 6-4章） */
    suspend fun getRoundEvents(): List<RoundEvent>

    suspend fun getRoundEvent(eventId: String): RoundEvent

    suspend fun createRoundEvent(
        clubName: String,
        datetime: Instant,
        fee: Int,
        capacity: Int
    ): RoundEvent

    /** 参加申請の作成。この時点では RoundEvent.current は加算されない */
    suspend fun applyJoin(eventId: String): RoundJoinRequest

    /** 主催者が自分の募集への参加申請一覧を取得する（created_by本人のみ） */
    suspend fun getJoinRequests(eventId: String): List<RoundJoinRequest>

    /** 参加承認。current加算・Connection生成はサーバー側で行われる */
    suspend fun approveJoinRequest(eventId: String, requestId: String): RoundJoinRequest

    suspend fun rejectJoinRequest(eventId: String, requestId: String): RoundJoinRequest
}
