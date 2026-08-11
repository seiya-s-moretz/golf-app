package com.golfmatch.app.domain.repository

import com.golfmatch.app.domain.model.Report
import com.golfmatch.app.domain.model.ReportDetail
import com.golfmatch.app.domain.model.ReportReasonCategory
import com.golfmatch.app.domain.model.ReportStatus
import com.golfmatch.app.domain.model.ReportSummary
import com.golfmatch.app.domain.model.ReportTargetType

/**
 * 通報のリポジトリ（技術設計書 6-8章・6-9章、ADR-0007）
 */
interface ReportRepository {
    suspend fun submitReport(
        targetType: ReportTargetType,
        targetId: String,
        reasonCategory: ReportReasonCategory,
        reasonText: String?
    ): Report

    /**
     * 通報一覧取得（管理者向け、技術設計書6-9章 `GET /admin/reports`）。
     * [statusFilter] がnullの場合は全件取得。`is_admin=true`のユーザーのみ利用可能（サーバー側検証）。
     * [before] はページネーションカーソル（nullの場合は最新から取得）。
     */
    suspend fun getAdminReports(statusFilter: ReportStatus?, before: String?, limit: Int): List<ReportSummary>

    /** 通報詳細取得（管理者向け、技術設計書6-9章 `GET /admin/reports/{id}`） */
    suspend fun getAdminReportDetail(reportId: String): ReportDetail

    /**
     * 通報ステータスの変更（管理者向け、技術設計書6-9章 `PATCH /admin/reports/{id}/status`）。
     * 状態遷移順序の強制は行わない（過剰設計を避ける方針）。
     */
    suspend fun updateReportStatus(reportId: String, status: ReportStatus, handlingMemo: String?): ReportDetail
}
