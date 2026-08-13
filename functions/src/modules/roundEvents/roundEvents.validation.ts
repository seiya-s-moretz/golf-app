import { z } from "zod";

/**
 * ISO-8601（オフセット必須）の日時文字列。
 *
 * `Date.parse()`は`2026-08-13 10:00`や`2026/08/13`のような表記も受理してしまうが、Androidクライアントは
 * `kotlinx.datetime.Instant.parse()`で解釈しており（`RoundEventMapper.kt`）、これらは例外になる。
 * `GET /round-events`はリストを一括変換するため、**1件でも不正な形式が保存されると一覧全体が
 * 全ユーザーで表示できなくなる**。入口で厳密に弾く。
 */
const ISO8601_WITH_OFFSET = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?(\.\d+)?(Z|[+-]\d{2}:\d{2})$/;

/** ラウンド募集の上限値。他スキーマ（age, average_score, content長）と同様に非現実的な値を弾く。 */
const MAX_CLUB_NAME_LENGTH = 100;
const MAX_FEE = 1_000_000;
const MAX_CAPACITY = 100;

/** `POST /round-events`リクエストボディ（技術設計書6-4章）。 */
export const createRoundEventSchema = z.object({
  club_name: z
    .string()
    .min(1, "club_nameを入力してください")
    .max(MAX_CLUB_NAME_LENGTH, `club_nameは${MAX_CLUB_NAME_LENGTH}文字以内で指定してください`),
  datetime: z
    .string()
    .regex(ISO8601_WITH_OFFSET, "datetimeはISO-8601形式（オフセット付き。例: 2026-09-01T09:00:00+09:00）で指定してください")
    .refine((v) => !Number.isNaN(Date.parse(v)), "datetimeが日時として解釈できません"),
  fee: z.number().int().min(0, "feeは0以上で指定してください").max(MAX_FEE, `feeは${MAX_FEE}以下で指定してください`),
  capacity: z
    .number()
    .int()
    .min(1, "capacityは1以上で指定してください")
    .max(MAX_CAPACITY, `capacityは${MAX_CAPACITY}以下で指定してください`),
});
