import { Timestamp } from "firebase-admin/firestore";
import { db } from "../../config/firebaseAdmin";
import { AppError } from "../../lib/AppError";
import { newId } from "../../lib/ids";
import type { ReportDoc, ReportReasonCategory, ReportTargetType } from "../../types/firestore";

export interface ReportResponse {
  report_id: string;
  reporter_user_id: string;
  target_type: ReportTargetType;
  target_id: string;
  reason_category: ReportReasonCategory;
  reason_text: string | null;
  status: ReportDoc["status"];
  created_at: string;
  handled_by_user_id: string | null;
  handled_at: string | null;
  handling_memo: string | null;
}

/** `ReportDoc` → APIレスポンス形（技術設計書6-8章）。`GET /admin/reports`系のレスポンスからも共用する。 */
export function toReportResponse(doc: ReportDoc): ReportResponse {
  return {
    report_id: doc.reportId,
    reporter_user_id: doc.reporterUserId,
    target_type: doc.targetType,
    target_id: doc.targetId,
    reason_category: doc.reasonCategory,
    reason_text: doc.reasonText,
    status: doc.status,
    created_at: doc.createdAt.toDate().toISOString(),
    handled_by_user_id: doc.handledByUserId,
    handled_at: doc.handledAt ? doc.handledAt.toDate().toISOString() : null,
    handling_memo: doc.handlingMemo,
  };
}

async function assertTargetExists(targetType: ReportTargetType, targetId: string): Promise<void> {
  const collection = targetType === "USER" ? "users" : "boardPosts";
  const snap = await db.collection(collection).doc(targetId).get();
  if (!snap.exists) {
    throw new AppError(
      404,
      "NOT_FOUND",
      targetType === "USER" ? "通報対象のユーザーが見つかりません" : "通報対象の投稿が見つかりません"
    );
  }
}

export interface CreateReportInput {
  target_type: ReportTargetType;
  target_id: string;
  reason_category: ReportReasonCategory;
  reason_text?: string;
}

/** `POST /reports`（技術設計書6-8章）。作成時点の`status`は常に`PENDING`。 */
export async function createReport(reporterUserId: string, input: CreateReportInput): Promise<ReportResponse> {
  await assertTargetExists(input.target_type, input.target_id);

  const reportId = newId();
  const doc: ReportDoc = {
    reportId,
    reporterUserId,
    targetType: input.target_type,
    targetId: input.target_id,
    reasonCategory: input.reason_category,
    reasonText: input.reason_text ?? null,
    status: "PENDING",
    createdAt: Timestamp.now(),
    handledByUserId: null,
    handledAt: null,
    handlingMemo: null,
  };
  await db.collection("reports").doc(reportId).set(doc);
  return toReportResponse(doc);
}
