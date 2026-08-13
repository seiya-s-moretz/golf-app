import type { Query } from "firebase-admin/firestore";
import { Timestamp } from "firebase-admin/firestore";
import { db } from "../../../config/firebaseAdmin";
import { AppError } from "../../../lib/AppError";
import { assertValidDocumentId } from "../../../lib/documentId";
import { applyBeforeCursor, parseLimit } from "../../../lib/pagination";
import { toReportResponse } from "../reports.service";
import type { BoardPostDoc, ReportDoc, ReportStatus, UserDoc } from "../../../types/firestore";

/**
 * 通報管理（簡易管理画面）API（技術設計書6-9章、ADR-0007）。`is_admin=true`のみ許可
 * （認可は`requireAdmin`ミドルウェアで担保。本モジュール内では検証しない）。
 */

export interface ReportAdminReporterResponse {
  user_id: string;
  name: string;
  icon_url: string;
}

export interface ReportAdminSummaryResponse {
  report_id: string;
  reporter_user_id: string;
  target_type: ReportDoc["targetType"];
  target_id: string;
  reason_category: ReportDoc["reasonCategory"];
  reason_text: string | null;
  status: ReportStatus;
  created_at: string;
  handled_by_user_id: string | null;
  handled_at: string | null;
  handling_memo: string | null;
  reporter: ReportAdminReporterResponse;
  target_summary: string;
}

export interface ReportAdminTargetUserResponse {
  user_id: string;
  name: string;
  icon_url: string;
  gender: string;
  age: number;
  introduction: string;
}

export interface ReportAdminTargetBoardPostResponse {
  post_id: string;
  user_id: string;
  author_name: string;
  content: string;
}

export interface ReportAdminTargetDetailResponse {
  user: ReportAdminTargetUserResponse | null;
  board_post: ReportAdminTargetBoardPostResponse | null;
}

export interface ReportAdminDetailResponse {
  report_id: string;
  reporter_user_id: string;
  target_type: ReportDoc["targetType"];
  target_id: string;
  reason_category: ReportDoc["reasonCategory"];
  reason_text: string | null;
  status: ReportStatus;
  created_at: string;
  handled_by_user_id: string | null;
  handled_at: string | null;
  handling_memo: string | null;
  reporter: ReportAdminReporterResponse;
  target_detail: ReportAdminTargetDetailResponse;
}

const UNKNOWN_REPORTER: ReportAdminReporterResponse = { user_id: "", name: "(不明なユーザー)", icon_url: "" };

function toReporterResponse(userId: string, user?: UserDoc): ReportAdminReporterResponse {
  if (!user) return { ...UNKNOWN_REPORTER, user_id: userId };
  return { user_id: user.userId, name: user.name, icon_url: user.iconUrl };
}

async function getManyDocs<T>(collection: string, ids: string[]): Promise<Map<string, T>> {
  const uniqueIds = [...new Set(ids)];
  if (uniqueIds.length === 0) return new Map();
  const snaps = await db.getAll(...uniqueIds.map((id) => db.collection(collection).doc(id)));
  const map = new Map<string, T>();
  snaps.forEach((s) => {
    if (s.exists) map.set(s.id, s.data() as T);
  });
  return map;
}

function buildTargetSummary(
  report: ReportDoc,
  usersById: Map<string, UserDoc>,
  postsById: Map<string, BoardPostDoc>
): string {
  if (report.targetType === "USER") {
    const user = usersById.get(report.targetId);
    return user ? user.name : "(削除されたユーザー)";
  }
  const post = postsById.get(report.targetId);
  if (!post) return "(削除された投稿)";
  const author = usersById.get(post.userId);
  const authorName = author ? author.name : "(不明なユーザー)";
  const EXCERPT_LENGTH = 30;
  const excerpt = post.content.length > EXCERPT_LENGTH ? `${post.content.slice(0, EXCERPT_LENGTH)}…` : post.content;
  return `${authorName}: ${excerpt}`;
}

export interface ListAdminReportsParams {
  status?: ReportStatus;
  before?: string;
  limit?: unknown;
}

/**
 * `GET /admin/reports`（技術設計書6-9章）。`created_at`降順。`target_summary`は非正規化せず、
 * 一覧取得のたびに関連User/BoardPostを`getAll()`でバッチ取得して合成する（技術設計書12-2-3章）。
 */
export async function listAdminReports(params: ListAdminReportsParams): Promise<ReportAdminSummaryResponse[]> {
  let query: Query = db.collection("reports");
  if (params.status) {
    query = query.where("status", "==", params.status);
  }
  query = query.orderBy("createdAt", "desc");
  query = applyBeforeCursor(query, params.before);
  query = query.limit(parseLimit(params.limit));

  const snap = await query.get();
  const reports = snap.docs.map((d) => d.data() as ReportDoc);
  if (reports.length === 0) return [];

  const reporterIds = reports.map((r) => r.reporterUserId);
  const userTargetIds = reports.filter((r) => r.targetType === "USER").map((r) => r.targetId);
  const postTargetIds = reports.filter((r) => r.targetType === "BOARD_POST").map((r) => r.targetId);

  const [reportersById, userTargetsById, postsById] = await Promise.all([
    getManyDocs<UserDoc>("users", reporterIds),
    getManyDocs<UserDoc>("users", userTargetIds),
    getManyDocs<BoardPostDoc>("boardPosts", postTargetIds),
  ]);
  const usersById = new Map<string, UserDoc>([...reportersById, ...userTargetsById]);

  // BOARD_POSTの投稿者名解決に必要な追加ユーザーをまとめて取得する
  const missingAuthorIds = [...postsById.values()].map((p) => p.userId).filter((id) => !usersById.has(id));
  const additionalAuthorsById = await getManyDocs<UserDoc>("users", missingAuthorIds);
  additionalAuthorsById.forEach((user, id) => usersById.set(id, user));

  return reports.map((report) => ({
    ...toReportResponse(report),
    reporter: toReporterResponse(report.reporterUserId, usersById.get(report.reporterUserId)),
    target_summary: buildTargetSummary(report, usersById, postsById),
  }));
}

async function getReportDocOrThrow(reportId: string): Promise<ReportDoc> {
  const snap = await db.collection("reports").doc(reportId).get();
  if (!snap.exists) throw new AppError(404, "NOT_FOUND", "通報が見つかりません");
  return snap.data() as ReportDoc;
}

/**
 * `GET /admin/reports/{id}`（技術設計書6-9章）。`target_detail`にUSERなら`phone_number`等の機微情報を含めない
 * （8章の非機微情報方針）。
 */
export async function getAdminReportDetail(reportId: string): Promise<ReportAdminDetailResponse> {
  const report = await getReportDocOrThrow(reportId);
  const reporterSnap = await db.collection("users").doc(report.reporterUserId).get();
  const reporter = toReporterResponse(
    report.reporterUserId,
    reporterSnap.exists ? (reporterSnap.data() as UserDoc) : undefined
  );

  let targetDetail: ReportAdminTargetDetailResponse = { user: null, board_post: null };
  if (report.targetType === "USER") {
    const targetSnap = await db.collection("users").doc(report.targetId).get();
    if (targetSnap.exists) {
      const u = targetSnap.data() as UserDoc;
      targetDetail = {
        user: { user_id: u.userId, name: u.name, icon_url: u.iconUrl, gender: u.gender, age: u.age, introduction: u.introduction },
        board_post: null,
      };
    }
  } else {
    const postSnap = await db.collection("boardPosts").doc(report.targetId).get();
    if (postSnap.exists) {
      const post = postSnap.data() as BoardPostDoc;
      const authorSnap = await db.collection("users").doc(post.userId).get();
      const authorName = authorSnap.exists ? (authorSnap.data() as UserDoc).name : "(不明なユーザー)";
      targetDetail = {
        user: null,
        board_post: { post_id: post.postId, user_id: post.userId, author_name: authorName, content: post.content },
      };
    }
  }

  return { ...toReportResponse(report), reporter, target_detail: targetDetail };
}

export interface UpdateReportStatusInput {
  status: ReportStatus;
  handling_memo?: string;
}

/**
 * `PATCH /admin/reports/{id}/status`（技術設計書6-9章、ADR-0007）。
 * 状態遷移順序の強制は行わない。`handling_memo`は指定時のみ上書きする。
 */
export async function updateReportStatus(
  reportId: string,
  adminUserId: string,
  input: UpdateReportStatusInput
): Promise<ReportAdminDetailResponse> {
  assertValidDocumentId(reportId, "通報ID");
  const ref = db.collection("reports").doc(reportId);
  const snap = await ref.get();
  if (!snap.exists) throw new AppError(404, "NOT_FOUND", "通報が見つかりません");

  // 状態遷移順序は強制しない（ADR-0007）ため、誤操作の巻き戻しでPENDINGへ戻すこともできる。
  // その際に対応者・対応日時が残っていると「未対応なのに対応済み日時がある」矛盾した表示になるため、
  // PENDINGへ戻すときは対応情報をクリアする
  const isHandled = input.status !== "PENDING";
  const update: Record<string, unknown> = {
    status: input.status,
    handledByUserId: isHandled ? adminUserId : null,
    handledAt: isHandled ? Timestamp.now() : null,
  };
  if (input.handling_memo !== undefined) {
    update.handlingMemo = input.handling_memo;
  }
  await ref.update(update);

  return getAdminReportDetail(reportId);
}
