package com.golfmatch.app.domain.repository

import com.golfmatch.app.domain.model.MatchRequest

/**
 * マッチング申請のリポジトリ（技術設計書 6-5章）
 */
interface MatchRepository {
    suspend fun sendMatchRequest(toUserId: String): MatchRequest

    suspend fun getMatchRequests(direction: MatchRequestDirection): List<MatchRequest>

    /** 承認。認可はto_user_id本人のみ（サーバー側で検証）。承認時にConnectionが生成される */
    suspend fun approveMatchRequest(matchRequestId: String): MatchRequest

    suspend fun rejectMatchRequest(matchRequestId: String): MatchRequest
}

enum class MatchRequestDirection {
    RECEIVED,
    SENT
}
