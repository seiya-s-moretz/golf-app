package com.golfmatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * ラウンド募集（技術設計書 5-1章）
 *
 * [current] は参加申請が「承認」された時点でのみ加算される（申請中は含めない。ADR-0001）。
 */
data class RoundEvent(
    val eventId: String,
    val clubName: String,
    val datetime: Instant,
    val fee: Int,
    val capacity: Int,
    val current: Int,
    val createdBy: String,
    val createdAt: Instant
)
