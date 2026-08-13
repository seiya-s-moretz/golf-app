import { Timestamp } from "firebase-admin/firestore";
import { db } from "../../config/firebaseAdmin";
import { AppError } from "../../lib/AppError";
import { newId } from "../../lib/ids";
import { parseLimit } from "../../lib/pagination";
import { assertNotBlocked, excludeBlockedUsers } from "../blocks/blocks.service";
import { ensureConnection } from "../connections/connections.service";
import { toUserResponses, type UserResponse } from "../users/users.service";
import type { MatchRequestDoc, MatchRequestStatus, UserDoc } from "../../types/firestore";

export interface MatchRequestResponse {
  match_request_id: string;
  from_user_id: string;
  to_user_id: string;
  status: MatchRequestStatus;
  created_at: string;
  responded_at: string | null;
}

function toMatchRequestResponse(doc: MatchRequestDoc): MatchRequestResponse {
  return {
    match_request_id: doc.matchRequestId,
    from_user_id: doc.fromUserId,
    to_user_id: doc.toUserId,
    status: doc.status,
    created_at: doc.createdAt.toDate().toISOString(),
    responded_at: doc.respondedAt ? doc.respondedAt.toDate().toISOString() : null,
  };
}

// レコメンドロジックの配点（要件定義書3-1章）。
const SCORE_DIFF_THRESHOLD = 10;
const SCORE_DIFF_POINTS = 40;
const AREA_MATCH_POINTS = 40;
const PURPOSE_MATCH_POINTS = 20;
const RECOMMEND_THRESHOLD = 60;

/**
 * 要件定義書3-1章のスコアリング式。
 * スコア差±10:40点／エリア一致:40点／目的一致:20点。
 */
function calculateRecommendScore(me: UserDoc, other: UserDoc): number {
  let score = 0;
  if (Math.abs(me.averageScore - other.averageScore) <= SCORE_DIFF_THRESHOLD) {
    score += SCORE_DIFF_POINTS;
  }
  if (me.areaId === other.areaId) {
    score += AREA_MATCH_POINTS;
  }
  if (me.purpose === other.purpose) {
    score += PURPOSE_MATCH_POINTS;
  }
  return score;
}

/**
 * `GET /users/recommend`（技術設計書6-5章、要件定義書3-1章）。
 * 自分以外の全ユーザーにスコアリングを適用し、60点以上のユーザーのみを返す。
 * スコア降順で返す（6章に順序の明記は無いが、推薦の趣旨上スコアの高い順が妥当と判断した。DeveloperAgent判断）。
 * ブロック関係（双方向）にあるユーザーを結果から除外する（Phase3で追加。技術設計書5-2章）。
 */
export interface ListRecommendedUsersParams {
  /** 前ページ最後のユーザーID（このユーザーの次から返す） */
  beforeId?: string;
  limit?: unknown;
}

export async function listRecommendedUsers(
  me: UserDoc,
  params: ListRecommendedUsersParams = {}
): Promise<UserResponse[]> {
  const candidates = await fetchRecommendCandidates(me);
  const scored = candidates
    .filter((u) => u.userId !== me.userId)
    // 停止中アカウントは`authenticate`が弾くため申請を承認できない。おすすめに出しても
    // 申請が宙に浮くだけなので除外する（技術設計書5-1章 User.status）
    .filter((u) => u.status === "ACTIVE")
    .map((u) => ({ user: u, score: calculateRecommendScore(me, u) }))
    .filter(({ score }) => score >= RECOMMEND_THRESHOLD)
    // スコア降順。同点はユーザーIDで固定し、ページ間で並びがぶれないようにする
    .sort((a, b) => b.score - a.score || a.user.userId.localeCompare(b.user.userId));

  const visible = await excludeBlockedUsers(scored, me.userId, ({ user }) => user.userId);

  // スコアはサーバー上の計算結果でありFirestoreに保存されていないため、他の一覧のような
  // `created_at`カーソルは使えない。前ページ最後のユーザーIDを目印にその次から返す。
  // 目印のユーザーが2回目の取得までに消えた場合（ブロック・停止など）は先頭から返すため、
  // クライアント側は`user_id`で重複排除してから追加する（`RecommendViewModel`参照）。
  const anchorIndex = params.beforeId ? visible.findIndex(({ user }) => user.userId === params.beforeId) : -1;
  const start = anchorIndex >= 0 ? anchorIndex + 1 : 0;
  const page = visible.slice(start, start + parseLimit(params.limit));

  // ユーザー1人ごとにエリアを引くとN+1になるため一括変換する（ページ分だけ変換する）
  return toUserResponses(
    page.map(({ user }) => user),
    me.userId
  );
}

/**
 * スコアリング対象の候補をクエリで絞り込む（`users`コレクションの全件走査を避ける）。
 *
 * 配点は「スコア差±10で40点／エリア一致で40点／目的一致で20点」、閾値は60点。
 * したがって**60点以上になるには3条件のうち2つ以上を満たす必要がある**。言い換えると、
 * 推薦対象は必ず次のどちらかに含まれる。
 *   (A) エリアが一致する（40点。残り20点以上はスコア差か目的で必ず埋まる）
 *   (B) エリアは違うが「スコア差±10（40点）＋目的一致（20点）」で60点に届く
 * この2本のクエリの和集合を候補とすれば、**全件走査しても結果は変わらない**（取りこぼしが無い）。
 * 60点未満の候補が混ざるのは構わない。最終的な採否は従来どおり[calculateRecommendScore]が決める
 * （スコアリングの正解は1か所のままにし、クエリは候補を狭めるだけに留める）。
 *
 * **配点・閾値を変更する場合は、この絞り込み条件も併せて見直すこと**（例えば「エリア一致だけで
 * 60点」に変えると、上記(A)(B)では拾えない候補が出る）。`recommend.test.ts`に回帰テストがある。
 */
async function fetchRecommendCandidates(me: UserDoc): Promise<UserDoc[]> {
  const [byArea, byPurposeAndScore] = await Promise.all([
    db.collection("users").where("areaId", "==", me.areaId).get(),
    db
      .collection("users")
      .where("purpose", "==", me.purpose)
      .where("averageScore", ">=", me.averageScore - SCORE_DIFF_THRESHOLD)
      .where("averageScore", "<=", me.averageScore + SCORE_DIFF_THRESHOLD)
      .get(),
  ]);

  const byUserId = new Map<string, UserDoc>();
  for (const doc of [...byArea.docs, ...byPurposeAndScore.docs]) {
    const user = doc.data() as UserDoc;
    byUserId.set(user.userId, user);
  }
  return [...byUserId.values()];
}

async function getTargetUserOrThrow(userId: string): Promise<UserDoc> {
  const snap = await db.collection("users").doc(userId).get();
  if (!snap.exists) throw new AppError(404, "NOT_FOUND", "ユーザーが見つかりません");
  return snap.data() as UserDoc;
}

/**
 * `POST /users/{id}/match-requests`（技術設計書6-5章）。マッチング申請を作成する（`status=PENDING`）。
 * 自分自身への申請は400、`(from_user_id, to_user_id)`のPENDING重複は409を返す（5-2章制約）。
 * ブロック関係にある場合はサーバー側で拒否する（Phase3で追加。技術設計書5-2章）。
 */
export async function createMatchRequest(fromUserId: string, toUserId: string): Promise<MatchRequestResponse> {
  if (fromUserId === toUserId) {
    throw new AppError(400, "VALIDATION_ERROR", "自分自身にマッチング申請はできません");
  }
  await getTargetUserOrThrow(toUserId);
  await assertNotBlocked(fromUserId, toUserId);

  const duplicateSnap = await db
    .collection("matchRequests")
    .where("fromUserId", "==", fromUserId)
    .where("toUserId", "==", toUserId)
    .where("status", "==", "PENDING")
    .limit(1)
    .get();
  if (!duplicateSnap.empty) {
    throw new AppError(409, "CONFLICT", "既にこのユーザーへマッチング申請済みです");
  }

  const matchRequestId = newId();
  const now = Timestamp.now();
  const doc: MatchRequestDoc = {
    matchRequestId,
    fromUserId,
    toUserId,
    status: "PENDING",
    createdAt: now,
    respondedAt: null,
  };
  await db.collection("matchRequests").doc(matchRequestId).set(doc);
  return toMatchRequestResponse(doc);
}

/** `GET /users/me/match-requests?direction=received|sent`（技術設計書6-5章）。 */
export async function listMyMatchRequests(
  userId: string,
  direction: "received" | "sent"
): Promise<MatchRequestResponse[]> {
  const field = direction === "received" ? "toUserId" : "fromUserId";
  const snap = await db.collection("matchRequests").where(field, "==", userId).orderBy("createdAt", "desc").get();
  const requests = snap.docs.map((d) => d.data() as MatchRequestDoc);
  // ブロック関係にある相手の申請は一覧から除外する（他の一覧系APIと同じ扱い。技術設計書5-2章）。
  // 除外しないと、ブロックした相手からの申請が受信一覧に残り続け、承認もできてしまう
  const visible = await excludeBlockedUsers(requests, userId, (r) =>
    direction === "received" ? r.fromUserId : r.toUserId
  );
  return visible.map(toMatchRequestResponse);
}

/**
 * `POST /match-requests/{id}/approve`（技術設計書6-5章）。`Connection`を作成する。認可: `to_user_id`本人のみ。
 */
export async function approveMatchRequest(
  matchRequestId: string,
  requesterUserId: string
): Promise<MatchRequestResponse> {
  const requestRef = db.collection("matchRequests").doc(matchRequestId);

  const updated = await db.runTransaction(async (tx) => {
    const snap = await tx.get(requestRef);
    if (!snap.exists) throw new AppError(404, "NOT_FOUND", "マッチング申請が見つかりません");
    const request = snap.data() as MatchRequestDoc;

    if (request.toUserId !== requesterUserId) {
      throw new AppError(403, "FORBIDDEN", "この申請の宛先ユーザーのみ承認できます");
    }
    if (request.status !== "PENDING") {
      throw new AppError(409, "CONFLICT", "このマッチング申請は既に処理済みです");
    }
    // 申請時（createMatchRequest）以降にブロックされた可能性があるため、承認時にも再判定する。
    // これが無いと、ブロック関係のままConnectionが作られ、メッセージを送れない会話が
    // 双方の一覧に残り続ける（技術設計書5-2章）
    await assertNotBlocked(request.fromUserId, request.toUserId, tx);

    // Connectionの読み取り・（未作成なら）作成を、他の書き込みより前に完了させる
    // （Firestoreトランザクションは全ての読み取りを全ての書き込みより先に行う必要があるため。roundEvents.service.tsと同じパターン）。
    await ensureConnection(
      { userIdA: request.fromUserId, userIdB: request.toUserId, sourceType: "MATCH_REQUEST", sourceId: matchRequestId },
      tx
    );

    const now = Timestamp.now();
    tx.update(requestRef, { status: "ACCEPTED", respondedAt: now });
    const updatedRequest: MatchRequestDoc = { ...request, status: "ACCEPTED", respondedAt: now };
    return updatedRequest;
  });

  return toMatchRequestResponse(updated);
}

/** `POST /match-requests/{id}/reject`（技術設計書6-5章）。認可: `to_user_id`本人のみ。 */
export async function rejectMatchRequest(
  matchRequestId: string,
  requesterUserId: string
): Promise<MatchRequestResponse> {
  const requestRef = db.collection("matchRequests").doc(matchRequestId);

  const updated = await db.runTransaction(async (tx) => {
    const snap = await tx.get(requestRef);
    if (!snap.exists) throw new AppError(404, "NOT_FOUND", "マッチング申請が見つかりません");
    const request = snap.data() as MatchRequestDoc;

    if (request.toUserId !== requesterUserId) {
      throw new AppError(403, "FORBIDDEN", "この申請の宛先ユーザーのみ却下できます");
    }
    if (request.status !== "PENDING") {
      throw new AppError(409, "CONFLICT", "このマッチング申請は既に処理済みです");
    }

    const now = Timestamp.now();
    tx.update(requestRef, { status: "REJECTED", respondedAt: now });
    const updatedRequest: MatchRequestDoc = { ...request, status: "REJECTED", respondedAt: now };
    return updatedRequest;
  });

  return toMatchRequestResponse(updated);
}
