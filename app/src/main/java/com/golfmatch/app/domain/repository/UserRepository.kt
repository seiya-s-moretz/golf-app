package com.golfmatch.app.domain.repository

import com.golfmatch.app.domain.model.Purpose
import com.golfmatch.app.domain.model.User

/**
 * ユーザー・プロフィール関連のリポジトリ（技術設計書 6-3章）
 */
interface UserRepository {
    suspend fun getUser(userId: String): User

    suspend fun updateUser(
        userId: String,
        name: String,
        gender: String,
        age: Int,
        areaId: String,
        averageScore: Int,
        purpose: Purpose,
        introduction: String
    ): User

    /** おすすめユーザー一覧（ブロック関係にあるユーザーはサーバー側で除外済み、技術設計書 6-5章） */
    /**
     * おすすめユーザー（スコア降順）。[beforeId]は前ページ最後のユーザーID（nullなら先頭から）。
     * スコアはサーバー計算値のため時刻カーソルは使えず、目印のユーザーが消えた場合は先頭から返る。
     * 呼び出し側は`userId`で重複排除してから追加すること。
     */
    suspend fun getRecommendedUsers(beforeId: String?, limit: Int): List<User>

    suspend fun blockUser(userId: String)

    suspend fun unblockUser(userId: String)

    suspend fun getBlockedUsers(): List<User>
}
