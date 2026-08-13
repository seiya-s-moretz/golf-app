import { Router } from "express";
import { asyncHandler } from "../../middleware/asyncHandler";
import { authenticate } from "../../middleware/authenticate";
import { listConversationsQuerySchema, listMessagesQuerySchema, sendMessageSchema } from "./messaging.validation";
import { listConversations, listMessages, markConversationRead, sendMessage } from "./messaging.service";

/** メッセージルーティング（技術設計書6-7章）。`app.ts`で`/conversations`にマウントする。 */
export const conversationsRoutes = Router();

conversationsRoutes.use(authenticate);

// GET /conversations（技術設計書6-7章）
conversationsRoutes.get(
  "/",
  asyncHandler(async (req, res) => {
    const { before, before_id: beforeId, limit } = listConversationsQuerySchema.parse(req.query);
    res.json(await listConversations(req.currentUser!.userId, { before, beforeId, limit }));
  })
);

// GET /conversations/{partnerId}/messages（技術設計書6-7章）
conversationsRoutes.get(
  "/:partnerId/messages",
  asyncHandler(async (req, res) => {
    const { before, before_id: beforeId, limit } = listMessagesQuerySchema.parse(req.query);
    res.json(await listMessages(req.currentUser!.userId, req.params.partnerId, { before, beforeId }, limit));
  })
);

// POST /conversations/{partnerId}/messages（技術設計書6-7章）
conversationsRoutes.post(
  "/:partnerId/messages",
  asyncHandler(async (req, res) => {
    const { content } = sendMessageSchema.parse(req.body);
    const created = await sendMessage(req.currentUser!.userId, req.params.partnerId, content);
    res.status(201).json(created);
  })
);

// POST /conversations/{partnerId}/read（技術設計書6-7章）
conversationsRoutes.post(
  "/:partnerId/read",
  asyncHandler(async (req, res) => {
    await markConversationRead(req.currentUser!.userId, req.params.partnerId);
    res.status(204).send();
  })
);
