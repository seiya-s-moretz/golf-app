import { Timestamp } from "firebase-admin/firestore";
import { db } from "../../config/firebaseAdmin";
import { AppError } from "../../lib/AppError";
import { newId } from "../../lib/ids";
import { ensureConnection } from "../connections/connections.service";
import { toUserResponse, type UserResponse } from "../users/users.service";
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
 * ブロック関係の除外フィルタはPhase3で追加する（技術設計書13-3章の依存関係。今回は未実装）。
 */
export async function listRecommendedUsers(me: UserDoc): Promise<UserResponse[]> {
  const snap = await db.collection("users").get();
  const scored = snap.docs
    .map((d) => d.data() as UserDoc)
    .filter((u) => u.userId !== me.userId)
    .map((u) => ({ user: u, score: calculateRecommendScore(me, u) }))
    .filter(({ score }) => score >= RECOMMEND_THRESHOLD)
    .sort((a, b) => b.score - a.score);

  return Promise.all(scored.map(({ user }) => toUserResponse(user, me.userId)));
}

async function getTargetUserOrThrow(userId: string): Promise<UserDoc> {
  const snap = await db.collection("users").doc(userId).get();
  if (!snap.exists) throw new AppError(404, "NOT_FOUND", "ユーザーが見つかりません");
  return snap.data() as UserDoc;
}

/**
 * `POST /users/{id}/match-requests`（技術設計書6-5章）。マッチング申請を作成する（`status=PENDING`）。
 * 自分自身への申請は400、`(from_user_id, to_user_id)`のPENDING重複は409を返す（5-2章制約）。
 */
export async function createMatchRequest(fromUserId: string, toUserId: string): Promise<MatchRequestResponse> {
  if (fromUserId === toUserId) {
    throw new AppError(400, "VALIDATION_ERROR", "自分自身にマッチング申請はできません");
  }
  await getTargetUserOrThrow(toUserId);

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
  return snap.docs.map((d) => toMatchRequestResponse(d.data() as MatchRequestDoc));
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
