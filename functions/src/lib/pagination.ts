import type { Query } from "firebase-admin/firestore";
import { AppError } from "./AppError";

/**
 * カーソル型ページネーション（`before`/`limit`）の共通ヘルパー（技術設計書12-1章）。
 * `GET /admin/reports`・`GET /conversations/{partnerId}/messages`・`GET /board`で使用する。
 */
export const DEFAULT_PAGE_LIMIT = 20;
export const MAX_PAGE_LIMIT = 100;

/**
 * `limit`クエリパラメータを解釈する。未指定なら[DEFAULT_PAGE_LIMIT]、上限は[MAX_PAGE_LIMIT]。
 *
 * 数値として解釈できない値は**黙って既定値に丸めず400で弾く**。丸めてしまうと、クライアントの
 * 組み立てミス（`limit=undefined`等）に気づけないままページングが意図しない挙動になる。
 */
export function parseLimit(raw: unknown, fallback: number = DEFAULT_PAGE_LIMIT): number {
  if (raw === undefined || raw === null || raw === "") return fallback;
  const n = Number(raw);
  if (!Number.isFinite(n) || n <= 0) {
    throw new AppError(400, "VALIDATION_ERROR", "limitは1以上の数値で指定してください");
  }
  return Math.min(Math.trunc(n), MAX_PAGE_LIMIT);
}

/**
 * `before`カーソル（ISO-8601文字列）より前のドキュメントに絞り込む。
 *
 * 日時として解釈できない値は**黙って無視せず400で弾く**。無視するとカーソル指定が効かないまま
 * 1ページ目が返るため、ページングし続けるクライアントが同じページを取り続ける（無限ループ）。
 */
export function applyBeforeCursor<T extends Query>(query: T, before: string | undefined, field = "createdAt"): T {
  if (before === undefined || before === "") return query;
  const date = new Date(before);
  if (Number.isNaN(date.getTime())) {
    throw new AppError(400, "VALIDATION_ERROR", "beforeはISO-8601形式の日時で指定してください");
  }
  return query.where(field, "<", date) as T;
}
