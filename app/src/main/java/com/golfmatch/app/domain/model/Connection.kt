package com.golfmatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * メッセージ許可関係（技術設計書 5-2章、ADR-0004）
 *
 * マッチング申請の承認、またはラウンド参加申請の承認によって自動生成される、
 * 2ユーザー間のメッセージ許可関係。[userAId] < [userBId] となるよう正規化し重複生成を防ぐ。
 */
data class Connection(
    val connectionId: String,
    val userAId: String,
    val userBId: String,
    val sourceType: ConnectionSourceType,
    val sourceId: String,
    val createdAt: Instant
)

enum class ConnectionSourceType {
    MATCH_REQUEST,
    ROUND_JOIN
}
