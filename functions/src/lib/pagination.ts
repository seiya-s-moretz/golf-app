import type { Query } from "firebase-admin/firestore";

/**
 * カーソル型ページネーション（`before`/`limit`）の共通ヘルパー（技術設計書12-1章）。
 * Phase1の対象APIには一覧ページネーションを持つものが無いため現時点では未使用だが、
 * Phase3（`GET /admin/reports`, `GET /conversations/{partnerId}/messages`）で使う想定の
 * 共通基盤として12-1章の構成に合わせて用意しておく。
 */
export const DEFAULT_PAGE_LIMIT = 20;
export const MAX_PAGE_LIMIT = 100;

export function parseLimit(raw: unknown, fallback: number = DEFAULT_PAGE_LIMIT): number {
  const n = Number(raw);
  if (!Number.isFinite(n) || n <= 0) return fallback;
  return Math.min(Math.trunc(n), MAX_PAGE_LIMIT);
}

/** `before`カーソル（ISO-8601文字列）より前のドキュメントに絞り込む。 */
export function applyBeforeCursor<T extends Query>(query: T, before: string | undefined, field = "createdAt"): T {
  if (!before) return query;
  const date = new Date(before);
  if (Number.isNaN(date.getTime())) return query;
  return query.where(field, "<", date) as T;
}
