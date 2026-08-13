import { z } from "zod";

export const reportTargetTypeSchema = z.enum(["USER", "BOARD_POST"]);
export const reportReasonCategorySchema = z.enum([
  "SPAM",
  "DATING_SOLICITATION",
  "HARASSMENT",
  "INAPPROPRIATE_CONTENT",
  "OTHER",
]);

/** `POST /reports`リクエストボディ（技術設計書6-8章・5-2章）。`reason_category=OTHER`時は`reason_text`必須。 */
export const createReportSchema = z
  .object({
    target_type: reportTargetTypeSchema,
    // `/`を含むIDはFirestoreの`doc()`が例外を投げ500になるため入口で弾く（lib/documentId.tsと同じ理由）
    target_id: z
      .string()
      .min(1, "target_idを指定してください")
      .refine((v) => !v.includes("/"), "target_idの形式が正しくありません"),
    reason_category: reportReasonCategorySchema,
    // 上限が無いと運営の通報管理画面に巨大なテキストがそのまま表示される
    reason_text: z.string().max(1000, "reason_textは1000文字以内で入力してください").optional(),
  })
  .refine((data) => data.reason_category !== "OTHER" || !!data.reason_text?.trim(), {
    message: "reason_category=OTHERの場合、reason_textは必須です",
    path: ["reason_text"],
  });

export type CreateReportBody = z.infer<typeof createReportSchema>;
