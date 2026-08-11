package com.golfmatch.app.domain.repository

import com.golfmatch.app.domain.model.BoardPost

/**
 * 掲示板のリポジトリ（技術設計書 6-6章）
 */
interface BoardRepository {
    /** ブロックしたユーザーの投稿はサーバー側で除外済み */
    suspend fun getBoardPosts(): List<BoardPost>

    suspend fun createBoardPost(content: String): BoardPost
}
