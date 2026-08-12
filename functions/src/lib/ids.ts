import { randomUUID } from "node:crypto";

/**
 * `crypto.randomUUID()`ラッパー（技術設計書12-1章）。
 * 5章のEntity定義（UUID型）との一貫性のため、Firestore自動採番IDは使わずUUIDを明示生成する。
 */
export function newId(): string {
  return randomUUID();
}
