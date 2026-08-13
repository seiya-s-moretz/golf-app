import type { Transaction } from "firebase-admin/firestore";
import { Timestamp } from "firebase-admin/firestore";
import { db } from "../../config/firebaseAdmin";
import { AppError } from "../../lib/AppError";
import { buildPairId, normalizePair } from "../../lib/pairId";
import type { ConnectionDoc, ConnectionSourceType } from "../../types/firestore";

/**
 * Connection共通モジュール（技術設計書12-1章）。Phase1で先行実装する（13-3章の依存関係）。
 * ラウンド参加承認（Phase1）・マッチング承認（Phase2）の双方から呼び出される想定。
 */

export interface EnsureConnectionParams {
  userIdA: string;
  userIdB: string;
  sourceType: ConnectionSourceType;
  sourceId: string;
}

/**
 * 2ユーザー間のConnectionを冪等に作成する（技術設計書5-2章・12-2-3章）。
 * 既にペアのConnectionが存在する場合は何もしない（重複生成防止）。
 *
 * `transaction`を渡した場合はそのトランザクション内でget/createを行う。呼び出し元のトランザクションで
 * 他の書き込み（例: RoundEvent.currentの加算）と合わせて呼ぶ場合は、Firestoreの制約
 * （トランザクション内は全ての読み取りを全ての書き込みより先に行う必要がある）を満たすため、
 * 他の`tx.update`/`tx.set`より前にこの関数を呼び出すこと。
 */
export async function ensureConnection(params: EnsureConnectionParams, transaction?: Transaction): Promise<void> {
  // 同一ユーザー同士のConnectionは`connections/{id}_{id}`という壊れたドキュメントになり、
  // 会話一覧に「自分との会話」が現れる一方でメッセージは送れない状態を作る。
  // 各呼び出し元でも自己申請を弾いているが、共通の入口としてここでも防ぐ
  if (params.userIdA === params.userIdB) {
    throw new AppError(400, "VALIDATION_ERROR", "同一ユーザー間のConnectionは作成できません");
  }
  const pairId = buildPairId(params.userIdA, params.userIdB);
  const ref = db.collection("connections").doc(pairId);
  const { userAId, userBId } = normalizePair(params.userIdA, params.userIdB);
  const doc: ConnectionDoc = {
    connectionId: pairId,
    userAId,
    userBId,
    sourceType: params.sourceType,
    sourceId: params.sourceId,
    createdAt: Timestamp.now(),
  };

  if (transaction) {
    const snap = await transaction.get(ref);
    if (snap.exists) return;
    transaction.create(ref, doc);
    return;
  }

  try {
    await ref.create(doc);
  } catch (err) {
    if (isAlreadyExistsError(err)) return;
    throw err;
  }
}

/** ユーザーペア間にConnectionが存在するかを取得する（Phase3のメッセージ認可チェック等で使用予定）。 */
export async function getConnection(userIdA: string, userIdB: string): Promise<ConnectionDoc | null> {
  const pairId = buildPairId(userIdA, userIdB);
  const snap = await db.collection("connections").doc(pairId).get();
  return snap.exists ? (snap.data() as ConnectionDoc) : null;
}

function isAlreadyExistsError(err: unknown): boolean {
  return typeof err === "object" && err !== null && "code" in err && (err as { code?: unknown }).code === 6;
}
