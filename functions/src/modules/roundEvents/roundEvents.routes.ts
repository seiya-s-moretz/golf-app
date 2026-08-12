import { Router } from "express";
import { asyncHandler } from "../../middleware/asyncHandler";
import { authenticate } from "../../middleware/authenticate";
import { createRoundEventSchema } from "./roundEvents.validation";
import {
  applyRoundJoin,
  approveJoinRequest,
  createRoundEvent,
  getRoundEvent,
  listJoinRequests,
  listRoundEvents,
  rejectJoinRequest,
} from "./roundEvents.service";

export const roundEventsRoutes = Router();

// 技術設計書6章冒頭の方針どおり、round-events配下は全エンドポイント認証必須（特記の無いものは認証必須）。
roundEventsRoutes.use(authenticate);

// GET /round-events（技術設計書6-4章。ブロック除外フィルタはPhase3で追加）
roundEventsRoutes.get(
  "/",
  asyncHandler(async (req, res) => {
    res.json(await listRoundEvents(req.currentUser!.userId));
  })
);

// POST /round-events（技術設計書6-4章）
roundEventsRoutes.post(
  "/",
  asyncHandler(async (req, res) => {
    const body = createRoundEventSchema.parse(req.body);
    const created = await createRoundEvent(req.currentUser!.userId, body);
    res.status(201).json(created);
  })
);

// GET /round-events/{id}
// 技術設計書6-4章には明記が無いが、AndroidクライアントApiService.ktが既に呼び出す前提で実装済みのため
// （ラウンド詳細取得）、追加実装した（roundEvents.service.ts参照）。
roundEventsRoutes.get(
  "/:id",
  asyncHandler(async (req, res) => {
    res.json(await getRoundEvent(req.params.id));
  })
);

// POST /round-events/{id}/join-requests（技術設計書6-4章）
roundEventsRoutes.post(
  "/:id/join-requests",
  asyncHandler(async (req, res) => {
    const created = await applyRoundJoin(req.params.id, req.currentUser!.userId);
    res.status(201).json(created);
  })
);

// GET /round-events/{id}/join-requests（技術設計書6-4章。主催者本人のみ許可）
roundEventsRoutes.get(
  "/:id/join-requests",
  asyncHandler(async (req, res) => {
    res.json(await listJoinRequests(req.params.id, req.currentUser!.userId));
  })
);

// POST /round-events/{id}/join-requests/{requestId}/approve（技術設計書6-4章）
roundEventsRoutes.post(
  "/:id/join-requests/:requestId/approve",
  asyncHandler(async (req, res) => {
    res.json(await approveJoinRequest(req.params.id, req.params.requestId, req.currentUser!.userId));
  })
);

// POST /round-events/{id}/join-requests/{requestId}/reject（技術設計書6-4章）
roundEventsRoutes.post(
  "/:id/join-requests/:requestId/reject",
  asyncHandler(async (req, res) => {
    res.json(await rejectJoinRequest(req.params.id, req.params.requestId, req.currentUser!.userId));
  })
);
