package com.golfmatch.app.domain.repository

import com.golfmatch.app.domain.model.BoardPost

/**
 * 掲示板のリポジトリ（技術設計書 6-6章）
 */
interface BoardRepository {
    /**
     * ブロックしたユーザーの投稿はサーバー側で除外済み。
     *
     * [before] はページネーションカーソル（nullの場合は最新から取得）。ブロック除外はサーバー側で
     * 取得後に行われるため、返却件数が[limit]未満でも次ページが存在しうる点に注意。
     */
    suspend fun getBoardPosts(before: String?, beforeId: String?, limit: Int): List<BoardPost>

    suspend fun createBoardPost(content: String): BoardPost
}
