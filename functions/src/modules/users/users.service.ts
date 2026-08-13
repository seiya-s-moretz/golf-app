import * as logger from "firebase-functions/logger";
import { db } from "../../config/firebaseAdmin";
import { AppError } from "../../lib/AppError";
import { assertValidDocumentId } from "../../lib/documentId";
import { getAreaById, getAreasByIds, toAreaResponse } from "../areas/areas.service";
import type { AreaMasterDoc, Purpose, UserDoc } from "../../types/firestore";

type AreaResponse = ReturnType<typeof toAreaResponse>;

export interface UserResponse {
  user_id: string;
  name: string;
  icon_url: string;
  gender: string;
  age: number;
  area: AreaResponse;
  average_score: number;
  purpose: Purpose;
  introduction: string;
  /** 本人閲覧時のみ含む（PII保護） */
  phone_number?: string;
  phone_verified: boolean;
  phone_verified_at: string | null;
  status: string;
  /** 本人閲覧時のみ含む（管理者アカウントの列挙を防ぐ） */
  is_admin?: boolean;
  created_at: string;
}

/**
 * UserDoc → APIレスポンス形（技術設計書6-3章。`area`はAreaMasterの参照展開）。
 *
 * `phone_number`（生の電話番号）はPII保護のため、閲覧者が対象ユーザー本人の場合のみ含める。
 * `viewerUserId`を省略した場合（新規登録直後・OTP再ログイン・本人によるプロフィール更新など、
 * レスポンスの受け手が常に本人であることが呼び出し元で保証されている場面）は本人閲覧として扱う。
 * `phone_verified`（真偽値）は電話番号自体ではないため、閲覧者を問わず常に含める。
 */
export async function toUserResponse(userDoc: UserDoc, viewerUserId?: string): Promise<UserResponse> {
  return buildUserResponse(userDoc, await getAreaById(userDoc.areaId), viewerUserId);
}

/**
 * 複数ユーザーをまとめてAPIレスポンス形へ変換する（技術設計書6-3章）。
 *
 * [toUserResponse]を`map`で回すと、ユーザー1人ごとにエリアマスタを1回読むN+1になる。
 * 一覧系API（おすすめ・会話一覧・ブロック一覧）は必ずこちらを使い、エリアは一括取得する。
 */
export async function toUserResponses(userDocs: UserDoc[], viewerUserId?: string): Promise<UserResponse[]> {
  const areaDocById = await getAreasByIds(userDocs.map((u) => u.areaId));
  return userDocs.map((userDoc) =>
    buildUserResponse(userDoc, areaDocById.get(userDoc.areaId) ?? null, viewerUserId)
  );
}

function buildUserResponse(userDoc: UserDoc, areaDoc: AreaMasterDoc | null, viewerUserId?: string): UserResponse {
  if (!areaDoc) {
    // エリアマスタの欠落はデータ不整合だが、ここで500を投げると
    // 一覧系API（会話一覧・ブロック一覧・おすすめ）が**1ユーザーの不整合で全体エラー**になる。
    // 他の参照欠落（会話相手の欠落・通報対象の欠落）と同様に縮退させ、調査用にログだけ残す
    logger.error("ユーザーが参照するエリアが見つかりません", {
      userId: userDoc.userId,
      areaId: userDoc.areaId,
    });
  }
  const isSelf = viewerUserId === undefined || viewerUserId === userDoc.userId;
  return {
    user_id: userDoc.userId,
    name: userDoc.name,
    icon_url: userDoc.iconUrl,
    gender: userDoc.gender,
    age: userDoc.age,
    area: areaDoc ? toAreaResponse(areaDoc) : unknownAreaResponse(userDoc.areaId),
    average_score: userDoc.averageScore,
    purpose: userDoc.purpose,
    introduction: userDoc.introduction,
    ...(isSelf ? { phone_number: userDoc.phoneNumber } : {}),
    phone_verified: userDoc.phoneVerified,
    phone_verified_at: userDoc.phoneVerifiedAt ? userDoc.phoneVerifiedAt.toDate().toISOString() : null,
    status: userDoc.status,
    // `is_admin`（運営メンバーかどうか）は本人以外には返さない。他人にも返すと、任意の認証済みユーザーが
    // 「どのアカウントが管理者か」を列挙でき、攻撃対象を特定されてしまう。
    // クライアント側は他人のプロフィールで`is_admin`を参照しない（マイページの自分自身のみ）ため影響は無い
    ...(isSelf ? { is_admin: userDoc.isAdmin } : {}),
    created_at: userDoc.createdAt.toDate().toISOString(),
  };
}

/** エリアマスタが欠落している場合の縮退表示用。クライアントは`area`を必須として扱うためnullにはしない。 */
function unknownAreaResponse(areaId: string): AreaResponse {
  return {
    area_id: areaId,
    prefecture: "",
    area_name: "(不明なエリア)",
    display_order: Number.MAX_SAFE_INTEGER,
    is_active: false,
    created_at: new Date(0).toISOString(),
  };
}

/** `GET /users/{id}`（技術設計書6-3章）。 */
export async function getUserById(userId: string): Promise<UserDoc> {
  assertValidDocumentId(userId, "ユーザーID");
  const snap = await db.collection("users").doc(userId).get();
  if (!snap.exists) throw new AppError(404, "NOT_FOUND", "ユーザーが見つかりません");
  return snap.data() as UserDoc;
}

export interface UpdateUserInput {
  name: string;
  gender: string;
  age: number;
  area_id: string;
  average_score: number;
  purpose: Purpose;
  introduction: string;
}

/**
 * `PUT /users/{id}`（技術設計書6-3章）。
 *
 * 技術設計書6章には認可要件の明記が無いが、本人以外のプロフィール編集を許可すると任意ユーザーが
 * 他人のプロフィールを書き換えられてしまうため、認証済みユーザー本人（`requesterUserId===targetUserId`）
 * にのみ許可する実装とした（DeveloperAgent判断）。他の書き込み系API（承認系等）が軒並み
 * 「本人のみ」等の認可要件を明記する中で本APIのみ記載が無いのは記載漏れの可能性があり、
 * ArchitectAgentへの確認を推奨する（実装メモ参照）。
 */
export async function updateUser(
  targetUserId: string,
  requesterUserId: string,
  input: UpdateUserInput
): Promise<UserDoc> {
  if (targetUserId !== requesterUserId) {
    throw new AppError(403, "FORBIDDEN", "本人以外のプロフィールは編集できません");
  }

  const ref = db.collection("users").doc(targetUserId);
  const snap = await ref.get();
  if (!snap.exists) throw new AppError(404, "NOT_FOUND", "ユーザーが見つかりません");

  const area = await getAreaById(input.area_id);
  if (!area || !area.isActive) {
    throw new AppError(400, "VALIDATION_ERROR", "指定されたエリアは選択できません");
  }

  await ref.update({
    name: input.name,
    gender: input.gender,
    age: input.age,
    areaId: input.area_id,
    averageScore: input.average_score,
    purpose: input.purpose,
    introduction: input.introduction,
  });

  const updatedSnap = await ref.get();
  return updatedSnap.data() as UserDoc;
}
