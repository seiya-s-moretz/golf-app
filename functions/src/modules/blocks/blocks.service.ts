import type { Transaction } from "firebase-admin/firestore";
import { Timestamp } from "firebase-admin/firestore";
import { db } from "../../config/firebaseAdmin";
import { AppError } from "../../lib/AppError";
import { toUserResponse, type UserResponse } from "../users/users.service";
import type { BlockDoc, UserDoc } from "../../types/firestore";

/**
 * ブロック関係の共通モジュール（技術設計書5-2章・6-3章・6-8章・12-2-3章・12-4章）。
 *
 * 「ブロック関係にあるか」の判定ロジックはここに一元化し、roundEvents/matching/board/messagingの
 * 各モジュールから再利用する（重複実装の回避）。技術設計書12-4章は`assertNotBlocked`を
 * `connections`モジュールに置く案を示しているが、`blocks`コレクションへのアクセスは全て本モジュールに
 * 閉じ込めたほうが責務が明確なため、`blocks`モジュール側に実装する（DeveloperAgent実装判断。
 * 挙動・シグネチャは12-4章の記述と同一）。
 */

function blockDocId(blockerUserId: string, blockedUserId: string): string {
  return `${blockerUserId}_${blockedUserId}`;
}

/** `POST /users/{id}/block`（技術設計書6-3章）。 */
export async function blockUser(blockerUserId: string, blockedUserId: string): Promise<void> {
  if (blockerUserId === blockedUserId) {
    throw new AppError(400, "VALIDATION_ERROR", "自分自身をブロックすることはできません");
  }
  const targetSnap = await db.collection("users").doc(blockedUserId).get();
  if (!targetSnap.exists) {
    throw new AppError(404, "NOT_FOUND", "ユーザーが見つかりません");
  }

  const doc: BlockDoc = {
    blockId: blockDocId(blockerUserId, blockedUserId),
    blockerUserId,
    blockedUserId,
    createdAt: Timestamp.now(),
  };
  // 既にブロック済みの場合も冪等に成功させる（6章に重複ブロック時のエラー要件の明記が無く、
  // UX上も再ブロックをエラーにする必要は薄いため。DeveloperAgent実装判断）。
  await db.collection("blocks").doc(doc.blockId).set(doc);
}

/** `DELETE /users/{id}/block`（技術設計書6-3章）。存在しない場合も冪等に成功させる。 */
export async function unblockUser(blockerUserId: string, blockedUserId: string): Promise<void> {
  await db.collection("blocks").doc(blockDocId(blockerUserId, blockedUserId)).delete();
}

/** `GET /users/me/blocks`（技術設計書6-3章）。自分がブロックしたユーザー一覧を返す。 */
export async function listBlockedUsers(blockerUserId: string): Promise<UserResponse[]> {
  const snap = await db.collection("blocks").where("blockerUserId", "==", blockerUserId).get();
  const blockedIds = snap.docs.map((d) => (d.data() as BlockDoc).blockedUserId);
  if (blockedIds.length === 0) return [];

  const userSnaps = await db.getAll(...blockedIds.map((id) => db.collection("users").doc(id)));
  const users = userSnaps.filter((s) => s.exists).map((s) => s.data() as UserDoc);
  return Promise.all(users.map((u) => toUserResponse(u, blockerUserId)));
}

/**
 * 2ユーザー間にブロック関係（双方向いずれか）があるかを判定する（技術設計書12-2-3章）。
 * `blocks/{A}_{B}`・`blocks/{B}_{A}`への2点Getで判定する（クエリ不要）。
 * `transaction`を渡した場合はそのトランザクション内のGetとして扱う（他の書き込みより前に呼び出すこと）。
 */
export async function isBlockedEitherDirection(
  userIdA: string,
  userIdB: string,
  transaction?: Transaction
): Promise<boolean> {
  const refAB = db.collection("blocks").doc(blockDocId(userIdA, userIdB));
  const refBA = db.collection("blocks").doc(blockDocId(userIdB, userIdA));
  const [snapAB, snapBA] = transaction
    ? await Promise.all([transaction.get(refAB), transaction.get(refBA)])
    : await Promise.all([refAB.get(), refBA.get()]);
  return snapAB.exists || snapBA.exists;
}

/**
 * ブロック関係にある場合は403(`BLOCKED`)を投げる（技術設計書5-2章・12-4章）。
 * マッチング申請作成・ラウンド参加申請作成・メッセージ送信の各serviceから共通で呼び出す。
 */
export async function assertNotBlocked(userIdA: string, userIdB: string, transaction?: Transaction): Promise<void> {
  if (await isBlockedEitherDirection(userIdA, userIdB, transaction)) {
    throw new AppError(403, "BLOCKED", "ブロック関係にあるユーザーのため実行できません");
  }
}

/**
 * `userId`が関わるブロック関係（双方向）にある相手ユーザーIDの集合を取得する（技術設計書12-2-3章）。
 * `blocker_user_id==me`・`blocked_user_id==me`の2クエリのUnionで求め、`not-in`演算子は使わない
 * （MVP規模のため。12-2-3章参照）。
 */
export async function getBlockedUserIdSet(userId: string): Promise<Set<string>> {
  const [blockedByMe, blockingMe] = await Promise.all([
    db.collection("blocks").where("blockerUserId", "==", userId).get(),
    db.collection("blocks").where("blockedUserId", "==", userId).get(),
  ]);
  const set = new Set<string>();
  blockedByMe.docs.forEach((d) => set.add((d.data() as BlockDoc).blockedUserId));
  blockingMe.docs.forEach((d) => set.add((d.data() as BlockDoc).blockerUserId));
  return set;
}

/**
 * 一覧系API（`GET /round-events`, `GET /users/recommend`, `GET /board`）共通のブロック除外フィルタ
 * （技術設計書12-2-3章・12-4章の`excludeBlockedUsers`）。`viewerUserId`とブロック関係（双方向）にある
 * ユーザーに紐づく要素をメモリ上で除外する。
 */
export async function excludeBlockedUsers<T>(
  items: T[],
  viewerUserId: string,
  getUserId: (item: T) => string
): Promise<T[]> {
  const blockedIds = await getBlockedUserIdSet(viewerUserId);
  if (blockedIds.size === 0) return items;
  return items.filter((item) => !blockedIds.has(getUserId(item)));
}
