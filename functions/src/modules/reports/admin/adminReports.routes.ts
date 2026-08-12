import { Router } from "express";
import { asyncHandler } from "../../../middleware/asyncHandler";
import { authenticate } from "../../../middleware/authenticate";
import { requireAdmin } from "../../../middleware/requireAdmin";
import { listAdminReportsQuerySchema, updateReportStatusSchema } from "./adminReports.validation";
import { getAdminReportDetail, listAdminReports, updateReportStatus } from "./adminReports.service";

/**
 * 通報管理（簡易管理画面）ルーティング（技術設計書6-9章、ADR-0007）。
 * `app.ts`で`/admin/reports`にマウントする。`is_admin=true`のみ許可（`requireAdmin`）。
 */
export const adminReportsRoutes = Router();

adminReportsRoutes.use(authenticate, requireAdmin);

// GET /admin/reports（技術設計書6-9章）
adminReportsRoutes.get(
  "/",
  asyncHandler(async (req, res) => {
    const { status, before, limit } = listAdminReportsQuerySchema.parse(req.query);
    res.json(await listAdminReports({ status, before, limit }));
  })
);

// GET /admin/reports/{id}（技術設計書6-9章）
adminReportsRoutes.get(
  "/:id",
  asyncHandler(async (req, res) => {
    res.json(await getAdminReportDetail(req.params.id));
  })
);

// PATCH /admin/reports/{id}/status（技術設計書6-9章）
adminReportsRoutes.patch(
  "/:id/status",
  asyncHandler(async (req, res) => {
    const body = updateReportStatusSchema.parse(req.body);
    res.json(await updateReportStatus(req.params.id, req.currentUser!.userId, body));
  })
);
