import { z } from "zod";

// メッセージ本文の最大文字数（技術設計書5-2章「目安500文字」に準拠。具体的な値の明記が無いため
// DeveloperAgent実装判断で500文字とする）。
export const MESSAGE_MAX_LENGTH = 500;

/** `POST /conversations/{partnerId}/messages`リクエストボディ（技術設計書6-7章）。 */
export const sendMessageSchema = z.object({
  content: z
    .string()
    .min(1, "contentを入力してください")
    .max(MESSAGE_MAX_LENGTH, `contentは${MESSAGE_MAX_LENGTH}文字以内で入力してください`),
});

/** `GET /conversations/{partnerId}/messages`クエリパラメータ（技術設計書6-7章）。 */
export const listMessagesQuerySchema = z.object({
  before: z.string().optional(),
  before_id: z.string().optional(),
  limit: z.string().optional(),
});
