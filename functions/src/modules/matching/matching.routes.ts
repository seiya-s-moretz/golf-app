import { Router } from "express";
import { asyncHandler } from "../../middleware/asyncHandler";
import { authenticate } from "../../middleware/authenticate";
import { listMatchRequestsQuerySchema } from "./matching.validation";
import {
  approveMatchRequest,
  createMatchRequest,
  listMyMatchRequests,
  listRecommendedUsers,
  rejectMatchRequest,
} from "./matching.service";

/**
 * おすすめユーザー・マッチング申請ルーティング（技術設計書6-5章）。
 *
 * `GET /users/recommend`・`GET /users/me/match-requests`・`POST /users/{id}/match-requests`は
 * `/users`配下のパスだが、Phase1の`usersRoutes`（変更禁止）が持つ`GET /users/:id`と
 * パスパターンが衝突しうる（`:id`に"recommend"や"me"が一致してしまう）。そのため本ルーターを
 * `app.ts`側で`usersRoutes`より先に`/users`へマウントすることで、該当パスをusersRoutesより先に処理させ
 * 衝突を回避する（DeveloperAgent実装判断。roundEvents/usersと同様「2層構成」のパターンを維持しつつ、
 * マウント順序のみで解決し、Phase1コードには一切手を入れない）。
 */
export const usersMatchingRoutes = Router();

// 注意: このルーターは`app.ts`で`/users`に（Phase1の`usersRoutes`より先に）マウントされ、
// `usersRoutes`宛のリクエスト（例: 認証不要の`POST /users`新規登録）もこのルーターを一度通過する。
// `router.use(authenticate)`のようなパス指定なしのミドルウェアを使うと、このルーター内のどのパスにも
// マッチしないリクエストに対しても認証チェックが先に走ってしまい、認証不要な`POST /users`が401になる
// （実装時に発生させた不具合。DeveloperAgentが動作確認で検出し修正）。そのため`authenticate`は
// 各ルートに個別に付与し、マッチしないリクエストは認証チェックなしで`usersRoutes`へフォールスルーさせる。

// GET /users/recommend（技術設計書6-5章、要件定義書3-1章のスコアリング）
usersMatchingRoutes.get(
  "/recommend",
  authenticate,
  asyncHandler(async (req, res) => {
    res.json(await listRecommendedUsers(req.currentUser!.doc));
  })
);

// GET /users/me/match-requests?direction=received|sent（技術設計書6-5章）
usersMatchingRoutes.get(
  "/me/match-requests",
  authenticate,
  asyncHandler(async (req, res) => {
    const { direction } = listMatchRequestsQuerySchema.parse(req.query);
    res.json(await listMyMatchRequests(req.currentUser!.userId, direction));
  })
);

// POST /users/{id}/match-requests（技術設計書6-5章）
usersMatchingRoutes.post(
  "/:id/match-requests",
  authenticate,
  asyncHandler(async (req, res) => {
    const created = await createMatchRequest(req.currentUser!.userId, req.params.id);
    res.status(201).json(created);
  })
);

/** `/match-requests`配下のルーティング（技術設計書6-5章）。`app.ts`で`/match-requests`にマウントする。 */
export const matchRequestsRoutes = Router();

matchRequestsRoutes.use(authenticate);

// POST /match-requests/{id}/approve（技術設計書6-5章。認可: to_user_id本人のみ）
matchRequestsRoutes.post(
  "/:id/approve",
  asyncHandler(async (req, res) => {
    res.json(await approveMatchRequest(req.params.id, req.currentUser!.userId));
  })
);

// POST /match-requests/{id}/reject（技術設計書6-5章。認可: to_user_id本人のみ）
matchRequestsRoutes.post(
  "/:id/reject",
  asyncHandler(async (req, res) => {
    res.json(await rejectMatchRequest(req.params.id, req.currentUser!.userId));
  })
);
