package com.golfmatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * 掲示板投稿（技術設計書 5-1章、変更なし）
 *
 * 一覧・詳細取得時、投稿者がブロック対象の場合はサーバー側でレスポンスから除外される。
 */
data class BoardPost(
    val postId: String,
    val userId: String,
    val content: String,
    val createdAt: Instant
)
