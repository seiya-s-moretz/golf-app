import { z } from "zod";

/**
 * `POST /board`リクエストボディ（技術設計書6-6章）。
 *
 * `content`の上限はメッセージ（500文字、`messaging.validation.ts`）より長めの掲示板向けに設定する。
 * 上限が無いと`express.json()`の上限（100KB）まで投稿でき、`GET /board`は全件を返すため
 * 巨大な投稿が数件あるだけで全ユーザーの一覧取得が重くなる。
 */
const MAX_CONTENT_LENGTH = 1000;

export const createBoardPostSchema = z.object({
  content: z
    .string()
    .min(1, "contentを入力してください")
    .max(MAX_CONTENT_LENGTH, `contentは${MAX_CONTENT_LENGTH}文字以内で入力してください`),
});
