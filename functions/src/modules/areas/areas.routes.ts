import { Router } from "express";
import { asyncHandler } from "../../middleware/asyncHandler";
import { listActiveAreas, toAreaResponse } from "./areas.service";

export const areasRoutes = Router();

// GET /areas（技術設計書6-2章。認証不要）
areasRoutes.get(
  "/",
  asyncHandler(async (_req, res) => {
    const areas = await listActiveAreas();
    res.json(areas.map(toAreaResponse));
  })
);
