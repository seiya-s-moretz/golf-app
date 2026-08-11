import { z } from "zod";

/** 目的タグのwire値（Androidクライアント`domain/model/User.kt`のPurpose enumと一致）。 */
export const purposeSchema = z.enum(["CASUAL", "SERIOUS", "LESSON_WANTED"]);

/** `PUT /users/{id}`リクエストボディ（技術設計書6-3章）。 */
export const updateUserSchema = z.object({
  name: z.string().min(1, "nameを入力してください"),
  gender: z.string().min(1, "genderを入力してください"),
  age: z.number().int().min(0).max(120, "ageは0〜120で指定してください"),
  area_id: z.string().min(1),
  average_score: z.number().int().min(40).max(200, "average_scoreは40〜200で指定してください"),
  purpose: purposeSchema,
  introduction: z.string(),
});

export type UpdateUserBody = z.infer<typeof updateUserSchema>;
