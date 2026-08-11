import { z } from "zod";

/** `POST /round-events`リクエストボディ（技術設計書6-4章）。 */
export const createRoundEventSchema = z.object({
  club_name: z.string().min(1, "club_nameを入力してください"),
  datetime: z
    .string()
    .refine((v) => !Number.isNaN(Date.parse(v)), "datetimeはISO-8601形式の日時文字列で指定してください"),
  fee: z.number().int().min(0, "feeは0以上で指定してください"),
  capacity: z.number().int().min(1, "capacityは1以上で指定してください"),
});
