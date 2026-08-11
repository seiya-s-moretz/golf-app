import { Router } from "express";
import { asyncHandler } from "../../middleware/asyncHandler";
import { authenticate } from "../../middleware/authenticate";
import { registerUser } from "../auth/auth.service";
import { registerUserSchema } from "../auth/auth.validation";
import { updateUserSchema } from "./users.validation";
import { getUserById, toUserResponse, updateUser } from "./users.service";

export const usersRoutes = Router();

// POST /users（新規登録。技術設計書6-1章、ADR-0003）。認証不要（registration_tokenが認証代わり）。
// プロフィール登録とアカウント作成を兼ねるためusersモジュールに配置しているが、
// 実処理（registration_tokenの検証・ユーザー作成・セッション発行）はauth.serviceに実装している
// （OTP検証で発行したregistration_tokenの消費ロジックと一体で管理するため）。
usersRoutes.post(
  "/",
  asyncHandler(async (req, res) => {
    const body = registerUserSchema.parse(req.body);
    const result = await registerUser(body);
    res.status(201).json(result);
  })
);

// GET /users/{id}（技術設計書6-3章）
usersRoutes.get(
  "/:id",
  authenticate,
  asyncHandler(async (req, res) => {
    const userDoc = await getUserById(req.params.id);
    res.json(await toUserResponse(userDoc));
  })
);

// PUT /users/{id}（技術設計書6-3章）
usersRoutes.put(
  "/:id",
  authenticate,
  asyncHandler(async (req, res) => {
    const body = updateUserSchema.parse(req.body);
    const updated = await updateUser(req.params.id, req.currentUser!.userId, body);
    res.json(await toUserResponse(updated));
  })
);
