import { db } from "../../config/firebaseAdmin";
import type { AreaMasterDoc } from "../../types/firestore";

export interface AreaResponse {
  area_id: string;
  prefecture: string;
  area_name: string;
  display_order: number;
  is_active: boolean;
  created_at: string;
}

/** AreaMasterDoc → APIレスポンス形（技術設計書6-2章、AndroidクライアントAreaDto.ktと一致させる）。 */
export function toAreaResponse(areaDoc: AreaMasterDoc): AreaResponse {
  return {
    area_id: areaDoc.areaId,
    prefecture: areaDoc.prefecture,
    area_name: areaDoc.areaName,
    display_order: areaDoc.displayOrder,
    is_active: areaDoc.isActive,
    created_at: areaDoc.createdAt.toDate().toISOString(),
  };
}

/** `GET /areas`（技術設計書6-2章）: `is_active=true`のみ、`display_order`昇順。 */
export async function listActiveAreas(): Promise<AreaMasterDoc[]> {
  const snap = await db.collection("areaMasters").where("isActive", "==", true).orderBy("displayOrder", "asc").get();
  return snap.docs.map((d) => d.data() as AreaMasterDoc);
}

/** `PUT /users/{id}`・`POST /users`の`area_id`バリデーション、`User.area`展開で使用。 */
export async function getAreaById(areaId: string): Promise<AreaMasterDoc | null> {
  const snap = await db.collection("areaMasters").doc(areaId).get();
  return snap.exists ? (snap.data() as AreaMasterDoc) : null;
}
