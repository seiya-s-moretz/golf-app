package com.golfmatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * ラウンド参加申請（技術設計書 5-2章、ADR-0001）
 *
 * 承認は [RoundEvent.capacity] > [RoundEvent.current] の場合のみ成立し、
 * 承認者は [RoundEvent.createdBy] と一致するユーザーのみ行える。
 */
data class RoundJoinRequest(
    val joinRequestId: String,
    val eventId: String,
    val userId: String,
    val status: RoundJoinRequestStatus,
    val createdAt: Instant,
    val respondedAt: Instant?
)

enum class RoundJoinRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
