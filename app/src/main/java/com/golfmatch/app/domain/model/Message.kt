package com.golfmatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * メッセージ（技術設計書 5-2章、ADR-0004）
 *
 * (userAId, userBId) の正規化ペアで会話をグルーピングする。
 */
data class Message(
    val messageId: String,
    val userAId: String,
    val userBId: String,
    val senderId: String,
    val content: String,
    val createdAt: Instant,
    val readAt: Instant?
)

/**
 * 会話一覧表示用の集約モデル（`GET /conversations` レスポンス相当、技術設計書 6-7章）
 */
data class Conversation(
    val partner: User,
    val lastMessage: Message?,
    val unreadCount: Int,
    val updatedAt: Instant
)
