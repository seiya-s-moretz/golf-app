import { Router } from "express";
import { asyncHandler } from "../../middleware/asyncHandler";
import { authenticate } from "../../middleware/authenticate";
import { createBoardPostSchema, listBoardPostsQuerySchema } from "./board.validation";
import { createBoardPost, listBoardPosts } from "./board.service";

export const boardRoutes = Router();

// 技術設計書6章冒頭の方針どおり、board配下は全エンドポイント認証必須（特記の無いものは認証必須）。
boardRoutes.use(authenticate);

// GET /board（技術設計書6-6章。ブロック除外フィルタはPhase3で追加、ページネーションは後日追加）
boardRoutes.get(
  "/",
  asyncHandler(async (req, res) => {
    const { before, before_id: beforeId, limit } = listBoardPostsQuerySchema.parse(req.query);
    res.json(await listBoardPosts(req.currentUser!.userId, { before, beforeId, limit }));
  })
);

// POST /board（技術設計書6-6章）
boardRoutes.post(
  "/",
  asyncHandler(async (req, res) => {
    const body = createBoardPostSchema.parse(req.body);
    const created = await createBoardPost(req.currentUser!.userId, body);
    res.status(201).json(created);
  })
);
