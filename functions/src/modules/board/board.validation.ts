import { z } from "zod";

/** `POST /board`リクエストボディ（技術設計書6-6章）。 */
export const createBoardPostSchema = z.object({
  content: z.string().min(1, "contentを入力してください"),
});
