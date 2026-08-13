import { z } from "zod";

export const reportStatusSchema = z.enum(["PENDING", "REVIEWING", "RESOLVED", "DISMISSED"]);

/** `GET /admin/reports`クエリパラメータ（技術設計書6-9章）。 */
export const listAdminReportsQuerySchema = z.object({
  status: reportStatusSchema.optional(),
  before: z.string().optional(),
  before_id: z.string().optional(),
  limit: z.string().optional(),
});

/**
 * `PATCH /admin/reports/{id}/status`リクエストボディ（技術設計書6-9章）。
 * `status`が列挙値以外の場合のみ400とし、状態遷移順序の強制は行わない（ADR-0007）。
 */
export const updateReportStatusSchema = z.object({
  status: reportStatusSchema,
  handling_memo: z.string().optional(),
});
