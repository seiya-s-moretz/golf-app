import { Timestamp } from "firebase-admin/firestore";
import { db } from "../../config/firebaseAdmin";
import { AppError } from "../../lib/AppError";
import { newId } from "../../lib/ids";
import { assertNotBlocked, excludeBlockedUsers } from "../blocks/blocks.service";
import { ensureConnection } from "../connections/connections.service";
import type { RoundEventDoc, RoundJoinRequestDoc, RoundJoinRequestStatus } from "../../types/firestore";

export interface RoundEventResponse {
  event_id: string;
  club_name: string;
  datetime: string;
  fee: number;
  capacity: number;
  current: number;
  created_by: string;
  created_at: string;
}

export interface RoundJoinRequestResponse {
  join_request_id: string;
  event_id: string;
  user_id: string;
  status: RoundJoinRequestStatus;
  created_at: string;
  responded_at: string | null;
}

function toRoundEventResponse(doc: RoundEventDoc): RoundEventResponse {
  return {
    event_id: doc.eventId,
    club_name: doc.clubName,
    datetime: doc.datetime,
    fee: doc.fee,
    capacity: doc.capacity,
    current: doc.current,
    created_by: doc.createdBy,
    created_at: doc.createdAt.toDate().toISOString(),
  };
}

function toJoinRequestResponse(doc: RoundJoinRequestDoc): RoundJoinRequestResponse {
  return {
    join_request_id: doc.joinRequestId,
    event_id: doc.eventId,
    user_id: doc.userId,
    status: doc.status,
    created_at: doc.createdAt.toDate().toISOString(),
    responded_at: doc.respondedAt ? doc.respondedAt.toDate().toISOString() : null,
  };
}

/**
 * `GET /round-events`（技術設計書6-4章）。
 * ブロック関係（双方向）にある作成者の募集をレスポンスから除外する（Phase3で追加。技術設計書5-2章・13-3章）。
 */
export async function listRoundEvents(requesterUserId: string): Promise<RoundEventResponse[]> {
  const snap = await db.collection("roundEvents").orderBy("createdAt", "desc").get();
  const events = snap.docs.map((d) => d.data() as RoundEventDoc);
  const visibleEvents = await excludeBlockedUsers(events, requesterUserId, (e) => e.createdBy);
  return visibleEvents.map(toRoundEventResponse);
}

export interface CreateRoundEventInput {
  club_name: string;
  datetime: string;
  fee: number;
  capacity: number;
}

/** `POST /round-events`（技術設計書6-4章）。 */
export async function createRoundEvent(
  createdBy: string,
  input: CreateRoundEventInput
): Promise<RoundEventResponse> {
  const eventId = newId();
  const now = Timestamp.now();
  const doc: RoundEventDoc = {
    eventId,
    clubName: input.club_name,
    datetime: input.datetime,
    fee: input.fee,
    capacity: input.capacity,
    current: 0,
    createdBy,
    createdAt: now,
  };
  await db.collection("roundEvents").doc(eventId).set(doc);
  return toRoundEventResponse(doc);
}

async function getRoundEventDocOrThrow(eventId: string): Promise<RoundEventDoc> {
  const snap = await db.collection("roundEvents").doc(eventId).get();
  if (!snap.exists) throw new AppError(404, "NOT_FOUND", "ラウンド募集が見つかりません");
  return snap.data() as RoundEventDoc;
}

/**
 * `GET /round-events/{id}`。
 * 技術設計書6-4章には明記が無いが、AndroidクライアントApiService.ktが既に呼び出す前提で実装済みのため
 * （`getRoundEvent()`）、RoundEventエンティティの単純な単体取得として追加実装した（DeveloperAgent判断）。
 * 挙動に疑義があればArchitectAgentへの確認を推奨する。
 */
export async function getRoundEvent(eventId: string): Promise<RoundEventResponse> {
  return toRoundEventResponse(await getRoundEventDocOrThrow(eventId));
}

/**
 * `POST /round-events/{id}/join-requests`（技術設計書6-4章）。
 * 主催者とブロック関係にある場合はサーバー側で拒否する（Phase3で追加。技術設計書5-2章）。
 */
export async function applyRoundJoin(eventId: string, userId: string): Promise<RoundJoinRequestResponse> {
  const eventRef = db.collection("roundEvents").doc(eventId);
  const eventSnap = await eventRef.get();
  if (!eventSnap.exists) throw new AppError(404, "NOT_FOUND", "ラウンド募集が見つかりません");
  const event = eventSnap.data() as RoundEventDoc;

  await assertNotBlocked(userId, event.createdBy);

  if (event.capacity <= event.current) {
    throw new AppError(409, "CONFLICT", "募集人数が上限に達しています");
  }

  const joinRequestsRef = eventRef.collection("joinRequests");
  const duplicateSnap = await joinRequestsRef
    .where("userId", "==", userId)
    .where("status", "==", "PENDING")
    .limit(1)
    .get();
  if (!duplicateSnap.empty) {
    throw new AppError(409, "CONFLICT", "既にこの募集へ参加申請済みです");
  }

  const requestId = newId();
  const now = Timestamp.now();
  const doc: RoundJoinRequestDoc = {
    joinRequestId: requestId,
    eventId,
    userId,
    status: "PENDING",
    createdAt: now,
    respondedAt: null,
  };
  await joinRequestsRef.doc(requestId).set(doc);
  return toJoinRequestResponse(doc);
}

/** `GET /round-events/{id}/join-requests`（技術設計書6-4章。主催者本人のみ許可）。 */
export async function listJoinRequests(
  eventId: string,
  requesterUserId: string
): Promise<RoundJoinRequestResponse[]> {
  const event = await getRoundEventDocOrThrow(eventId);
  if (event.createdBy !== requesterUserId) {
    throw new AppError(403, "FORBIDDEN", "この募集の主催者のみ参加申請を確認できます");
  }
  const snap = await db.collection("roundEvents").doc(eventId).collection("joinRequests").orderBy("createdAt", "desc").get();
  return snap.docs.map((d) => toJoinRequestResponse(d.data() as RoundJoinRequestDoc));
}

/**
 * `POST /round-events/{id}/join-requests/{requestId}/approve`（技術設計書6-4章）。
 * `capacity > current`を再検証のうえ`current`を加算し、`Connection`を作成する（主催者⇔申請者）。
 */
export async function approveJoinRequest(
  eventId: string,
  requestId: string,
  requesterUserId: string
): Promise<RoundJoinRequestResponse> {
  const eventRef = db.collection("roundEvents").doc(eventId);
  const requestRef = eventRef.collection("joinRequests").doc(requestId);

  const updated = await db.runTransaction(async (tx) => {
    const [eventSnap, requestSnap] = await Promise.all([tx.get(eventRef), tx.get(requestRef)]);
    if (!eventSnap.exists) throw new AppError(404, "NOT_FOUND", "ラウンド募集が見つかりません");
    if (!requestSnap.exists) throw new AppError(404, "NOT_FOUND", "参加申請が見つかりません");

    const event = eventSnap.data() as RoundEventDoc;
    const request = requestSnap.data() as RoundJoinRequestDoc;

    if (event.createdBy !== requesterUserId) {
      throw new AppError(403, "FORBIDDEN", "この募集の主催者のみ承認できます");
    }
    if (request.status !== "PENDING") {
      throw new AppError(409, "CONFLICT", "この参加申請は既に処理済みです");
    }
    if (event.capacity <= event.current) {
      throw new AppError(409, "CONFLICT", "募集人数が上限に達しているため承認できません");
    }

    // Connectionの読み取り・（未作成なら）作成を、他の書き込みより前に完了させる
    // （Firestoreトランザクションは全ての読み取りを全ての書き込みより先に行う必要があるため）。
    await ensureConnection(
      { userIdA: event.createdBy, userIdB: request.userId, sourceType: "ROUND_JOIN", sourceId: requestId },
      tx
    );

    const now = Timestamp.now();
    tx.update(eventRef, { current: event.current + 1 });
    tx.update(requestRef, { status: "APPROVED", respondedAt: now });

    const updatedRequest: RoundJoinRequestDoc = { ...request, status: "APPROVED", respondedAt: now };
    return updatedRequest;
  });

  return toJoinRequestResponse(updated);
}

/** `POST /round-events/{id}/join-requests/{requestId}/reject`（技術設計書6-4章）。 */
export async function rejectJoinRequest(
  eventId: string,
  requestId: string,
  requesterUserId: string
): Promise<RoundJoinRequestResponse> {
  const eventRef = db.collection("roundEvents").doc(eventId);
  const requestRef = eventRef.collection("joinRequests").doc(requestId);

  const updated = await db.runTransaction(async (tx) => {
    const [eventSnap, requestSnap] = await Promise.all([tx.get(eventRef), tx.get(requestRef)]);
    if (!eventSnap.exists) throw new AppError(404, "NOT_FOUND", "ラウンド募集が見つかりません");
    if (!requestSnap.exists) throw new AppError(404, "NOT_FOUND", "参加申請が見つかりません");

    const event = eventSnap.data() as RoundEventDoc;
    const request = requestSnap.data() as RoundJoinRequestDoc;

    if (event.createdBy !== requesterUserId) {
      throw new AppError(403, "FORBIDDEN", "この募集の主催者のみ却下できます");
    }
    if (request.status !== "PENDING") {
      throw new AppError(409, "CONFLICT", "この参加申請は既に処理済みです");
    }

    const now = Timestamp.now();
    tx.update(requestRef, { status: "REJECTED", respondedAt: now });

    const updatedRequest: RoundJoinRequestDoc = { ...request, status: "REJECTED", respondedAt: now };
    return updatedRequest;
  });

  return toJoinRequestResponse(updated);
}
