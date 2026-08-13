import type { Query } from "firebase-admin/firestore";
import { FieldPath, Timestamp } from "firebase-admin/firestore";
import { AppError } from "./AppError";

/**
 * カーソル型ページネーション（`before`/`before_id`/`limit`）の共通ヘルパー（技術設計書12-1章）。
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

export interface BeforeCursor {
  /** 前ページ最後の要素の`created_at`（ISO-8601） */
  before?: string;
  /** 前ページ最後の要素のドキュメントID */
  beforeId?: string;
}

/**
 * 前ページの最後の要素の**次**から取得する（`created_at`降順）。
 *
 * カーソルは`(created_at, ドキュメントID)`の組で受け取る。`created_at`だけで
 * `where("createdAt", "<", cursor)`とすると、**同じ時刻のドキュメントが丸ごと飛ばされる**
 * （ミリ秒精度のため、同時投稿・一括投入では実際に発生する）。ページ境界に同時刻の要素が
 * 並ぶと、後続ページのどれにも現れず永久に読めなくなるため、IDを第2ソートキーにして
 * `startAfter`で「その要素の次」を厳密に指す。
 *
 * `before`と`before_id`は必ず両方指定する（片方のみは400）。どちらも未指定なら先頭ページ。
 */
export function applyBeforeCursor<T extends Query>(query: T, cursor: BeforeCursor): T {
  const hasBefore = cursor.before !== undefined && cursor.before !== "";
  const hasBeforeId = cursor.beforeId !== undefined && cursor.beforeId !== "";

  // 第2ソートキーは常に付ける（先頭ページと後続ページで並び順が変わらないようにするため）
  const ordered = query.orderBy(FieldPath.documentId(), "desc") as T;
  if (!hasBefore && !hasBeforeId) return ordered;

  if (!hasBefore || !hasBeforeId) {
    throw new AppError(400, "VALIDATION_ERROR", "beforeとbefore_idは両方指定してください");
  }

  const date = new Date(cursor.before as string);
  if (Number.isNaN(date.getTime())) {
    throw new AppError(400, "VALIDATION_ERROR", "beforeはISO-8601形式の日時で指定してください");
  }
  return ordered.startAfter(Timestamp.fromDate(date), cursor.beforeId) as T;
}
