import express from "express";
import { errorHandler } from "./middleware/errorHandler";
import { areasRoutes } from "./modules/areas/areas.routes";
import { authRoutes } from "./modules/auth/auth.routes";
import { boardRoutes } from "./modules/board/board.routes";
import { matchRequestsRoutes, usersMatchingRoutes } from "./modules/matching/matching.routes";
import { roundEventsRoutes } from "./modules/roundEvents/roundEvents.routes";
import { usersRoutes } from "./modules/users/users.routes";

/**
 * Expressアプリの組み立て（技術設計書12-1章）。
 * `.../api/`という単一パス配下に全REST操作をぶら下げるAndroidクライアントの想定
 * （`NetworkModule.kt`のBASE_URL）に合わせ、Express Routerで機能ごとにルーティングを分ける。
 */
export function createApp(): express.Express {
  const app = express();
  app.use(express.json());

  app.use("/auth", authRoutes);
  app.use("/areas", areasRoutes);
  // usersMatchingRoutes（GET /users/recommend, GET /users/me/match-requests, POST /users/{id}/match-requests）は
  // Phase1のusersRoutes（変更禁止）が持つGET /users/:idとのパス衝突を避けるため、usersRoutesより先にマウントする
  // （技術設計書6-5章、matching.routes.ts参照）。
  app.use("/users", usersMatchingRoutes);
  app.use("/users", usersRoutes);
  app.use("/round-events", roundEventsRoutes);
  app.use("/match-requests", matchRequestsRoutes);
  app.use("/board", boardRoutes);

  app.use((req, res) => {
    res.status(404).json({
      error: {
        code: "NOT_FOUND",
        message: `リクエストされたエンドポイントは存在しません: ${req.method} ${req.path}`,
      },
    });
  });

  app.use(errorHandler);

  return app;
}
