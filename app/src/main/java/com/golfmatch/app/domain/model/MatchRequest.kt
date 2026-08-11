package com.golfmatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * マッチング申請（技術設計書 5-2章）
 *
 * (fromUserId, toUserId) の組み合わせでPENDING状態は1件まで（重複申請防止、サーバー側で担保）。
 */
data class MatchRequest(
    val matchRequestId: String,
    val fromUserId: String,
    val toUserId: String,
    val status: MatchRequestStatus,
    val createdAt: Instant,
    val respondedAt: Instant?
)

enum class MatchRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
