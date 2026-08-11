package com.golfmatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * ブロック（技術設計書 5-2章）
 *
 * ブロック関係にあるユーザーは、おすすめユーザー・掲示板から相互に非表示となり、
 * 新規のマッチング申請・ラウンド参加申請・メッセージ送信もサーバー側で拒否される。
 * 既存のConnection・メッセージ履歴は削除しない（証跡保全のため）。
 */
data class Block(
    val blockId: String,
    val blockerUserId: String,
    val blockedUserId: String,
    val createdAt: Instant
)
