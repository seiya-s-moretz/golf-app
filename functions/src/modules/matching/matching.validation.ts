import { z } from "zod";

/**
 * `GET /users/recommend`クエリパラメータ（技術設計書6-5章）。
 *
 * スコアはサーバー計算値でFirestoreに保存されないため、他の一覧のような`created_at`カーソルは使えない。
 * 前ページ最後のユーザーID（`before_id`）を目印にその次から返す。
 */
export const listRecommendedUsersQuerySchema = z.object({
  before_id: z.string().optional(),
  limit: z.string().optional(),
});

/** `GET /users/me/match-requests`クエリパラメータ（技術設計書6-5章）。 */
export const listMatchRequestsQuerySchema = z.object({
  direction: z.enum(["received", "sent"], {
    errorMap: () => ({ message: "directionはreceivedまたはsentで指定してください" }),
  }),
});
