import { z } from "zod";
import { purposeSchema } from "../users/users.validation";

const E164_REGEX = /^\+[1-9]\d{1,14}$/;

/** `POST /auth/phone/otp`リクエストボディ（技術設計書6-1章）。 */
export const requestOtpSchema = z.object({
  phone_number: z.string().regex(E164_REGEX, "phone_numberはE.164形式で指定してください（例: +819012345678）"),
});

/** `POST /auth/phone/verify`リクエストボディ（技術設計書6-1章）。 */
export const verifyOtpSchema = z.object({
  phone_number: z.string().regex(E164_REGEX, "phone_numberはE.164形式で指定してください"),
  otp_code: z.string().regex(/^\d{6}$/, "otp_codeは6桁の数字で指定してください"),
});

/** `POST /users`（新規登録）リクエストボディ（技術設計書6-1章）。 */
export const registerUserSchema = z.object({
  registration_token: z.string().min(1),
  name: z.string().min(1, "nameを入力してください"),
  gender: z.string().min(1, "genderを入力してください"),
  age: z.number().int().min(0).max(120, "ageは0〜120で指定してください"),
  area_id: z.string().min(1),
  average_score: z.number().int().min(40).max(200, "average_scoreは40〜200で指定してください"),
  purpose: purposeSchema,
  introduction: z.string(),
});
