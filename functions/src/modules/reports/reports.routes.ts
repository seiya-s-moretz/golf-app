import { Router } from "express";
import { asyncHandler } from "../../middleware/asyncHandler";
import { authenticate } from "../../middleware/authenticate";
import { createReportSchema } from "./reports.validation";
import { createReport } from "./reports.service";

/** 通報ルーティング（技術設計書6-8章）。`app.ts`で`/reports`にマウントする。 */
export const reportsRoutes = Router();

reportsRoutes.use(authenticate);

// POST /reports（技術設計書6-8章）
reportsRoutes.post(
  "/",
  asyncHandler(async (req, res) => {
    const body = createReportSchema.parse(req.body);
    const created = await createReport(req.currentUser!.userId, body);
    res.status(201).json(created);
  })
);
