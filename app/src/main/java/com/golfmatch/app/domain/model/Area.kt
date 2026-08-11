package com.golfmatch.app.domain.model

import kotlinx.datetime.Instant

/**
 * エリアマスタ（技術設計書 5-2章、ADR-0002）
 *
 * [isActive] がtrueのもののみ選択UIに表示する。falseにすることで
 * 「廃止」せず履歴を保持したままエリアを非表示にできる。
 */
data class Area(
    val areaId: String,
    val prefecture: String,
    val areaName: String,
    val displayOrder: Int,
    val isActive: Boolean,
    val createdAt: Instant
)
