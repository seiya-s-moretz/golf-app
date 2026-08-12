package com.golfmatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * ユーザー（技術設計書 5-1章）
 *
 * IDはFirestoreのドキュメントID（String）を用いる。
 */
data class User(
    val userId: String,
    val name: String,
    val iconUrl: String,
    val gender: String,
    val age: Int,
    val areaId: String,
    val averageScore: Int,
    val purpose: Purpose,
    val introduction: String,
    // 他人閲覧時はPII保護のためnull（本人閲覧時のみ非null）
    val phoneNumber: String?,
    val phoneVerified: Boolean,
    val phoneVerifiedAt: Instant?,
    val status: AccountStatus,
    val isAdmin: Boolean = false,
    val createdAt: Instant
)

/** 目的タグ（PRD 0章：恋愛要素を含まない3種のみ） */
enum class Purpose(val label: String) {
    CASUAL("わいわい"),
    SERIOUS("ガチ"),
    LESSON_WANTED("レクチャー求");

    companion object {
        /** サーバー側の表現（enum名 or 日本語ラベルのいずれか）から解決する */
        fun fromWireValue(value: String): Purpose =
            entries.firstOrNull { it.name == value || it.label == value }
                ?: throw IllegalArgumentException("Unknown purpose value: $value")
    }
}

/** アカウント状態。通報対応で運営が凍結する場合はSUSPENDEDとなる */
enum class AccountStatus {
    ACTIVE,
    SUSPENDED
}
