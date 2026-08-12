import { Router } from "express";
import { asyncHandler } from "../../middleware/asyncHandler";
import { authenticate } from "../../middleware/authenticate";
import { blockUser, listBlockedUsers, unblockUser } from "./blocks.service";

/**
 * ブロックルーティング（技術設計書6-3章・6-8章）。`/users`配下だが、Phase1の`usersRoutes`
 * （`GET/PUT /users/:id`）とのパス衝突を避けるため、matching.routes.tsと同じパターンで
 * `app.ts`側で`usersRoutes`より先に`/users`へマウントする。
 *
 * このルーターは`usersRoutes`宛のリクエスト（例: 認証不要の`POST /users`新規登録）も一度通過するため、
 * matching.routes.tsと同じ理由で`router.use(authenticate)`は使わず、各ルートに個別に`authenticate`を
 * 付与する（パス指定なしの`use`はどのパスにもマッチしないリクエストにも先に適用されてしまい、
 * 認証不要な`POST /users`が401になる不具合を招くため）。
 */
export const usersBlockRoutes = Router();

// GET /users/me/blocks（技術設計書6-3章）
usersBlockRoutes.get(
  "/me/blocks",
  authenticate,
  asyncHandler(async (req, res) => {
    res.json(await listBlockedUsers(req.currentUser!.userId));
  })
);

// POST /users/{id}/block（技術設計書6-3章）
usersBlockRoutes.post(
  "/:id/block",
  authenticate,
  asyncHandler(async (req, res) => {
    await blockUser(req.currentUser!.userId, req.params.id);
    res.status(204).send();
  })
);

// DELETE /users/{id}/block（技術設計書6-3章）
usersBlockRoutes.delete(
  "/:id/block",
  authenticate,
  asyncHandler(async (req, res) => {
    await unblockUser(req.currentUser!.userId, req.params.id);
    res.status(204).send();
  })
);
