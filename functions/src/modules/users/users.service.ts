import { db } from "../../config/firebaseAdmin";
import { AppError } from "../../lib/AppError";
import { getAreaById, toAreaResponse } from "../areas/areas.service";
import type { Purpose, UserDoc } from "../../types/firestore";

export interface UserResponse {
  user_id: string;
  name: string;
  icon_url: string;
  gender: string;
  age: number;
  area: ReturnType<typeof toAreaResponse>;
  average_score: number;
  purpose: Purpose;
  introduction: string;
  phone_number: string;
  phone_verified: boolean;
  phone_verified_at: string | null;
  status: string;
  is_admin: boolean;
  created_at: string;
}

/** UserDoc → APIレスポンス形（技術設計書6-3章。`area`はAreaMasterの参照展開）。 */
export async function toUserResponse(userDoc: UserDoc): Promise<UserResponse> {
  const areaDoc = await getAreaById(userDoc.areaId);
  if (!areaDoc) {
    // areaは業務上必須参照のため、欠落はサーバー内部の不整合として扱う（レスポンスからのarea欠落は許容しない）
    throw new AppError(
      500,
      "INTERNAL",
      `ユーザー ${userDoc.userId} が参照するエリア(${userDoc.areaId})が見つかりません`
    );
  }
  return {
    user_id: userDoc.userId,
    name: userDoc.name,
    icon_url: userDoc.iconUrl,
    gender: userDoc.gender,
    age: userDoc.age,
    area: toAreaResponse(areaDoc),
    average_score: userDoc.averageScore,
    purpose: userDoc.purpose,
    introduction: userDoc.introduction,
    phone_number: userDoc.phoneNumber,
    phone_verified: userDoc.phoneVerified,
    phone_verified_at: userDoc.phoneVerifiedAt ? userDoc.phoneVerifiedAt.toDate().toISOString() : null,
    status: userDoc.status,
    is_admin: userDoc.isAdmin,
    created_at: userDoc.createdAt.toDate().toISOString(),
  };
}

/** `GET /users/{id}`（技術設計書6-3章）。 */
export async function getUserById(userId: string): Promise<UserDoc> {
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
