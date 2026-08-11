package com.golfmatch.app.domain.repository

import com.golfmatch.app.domain.model.Area

/**
 * エリアマスタのリポジトリ（技術設計書 6-2章、ADR-0002）
 */
interface AreaRepository {
    /** 選択可能なエリア一覧（is_active=true のみ、display_order昇順）。認証不要 */
    suspend fun getAreas(): List<Area>
}
