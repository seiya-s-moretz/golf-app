import type { Query } from "firebase-admin/firestore";
import { Timestamp } from "firebase-admin/firestore";
import { db } from "../../config/firebaseAdmin";
import { newId } from "../../lib/ids";
import { applyBeforeCursor, parseLimit } from "../../lib/pagination";
import { excludeBlockedUsers } from "../blocks/blocks.service";
import type { BoardPostDoc } from "../../types/firestore";

export interface BoardPostResponse {
  post_id: string;
  user_id: string;
  content: string;
  created_at: string;
}

function toBoardPostResponse(doc: BoardPostDoc): BoardPostResponse {
  return {
    post_id: doc.postId,
    user_id: doc.userId,
    content: doc.content,
    created_at: doc.createdAt.toDate().toISOString(),
  };
}

export interface ListBoardPostsParams {
  before?: string;
  limit?: unknown;
}

/**
 * `GET /board`（技術設計書6-6章）。`created_at`降順・カーソル型ページネーション（`before`/`limit`）。
 *
 * 当初は全件取得だったが、掲示板は投稿が増え続ける一方であり、全件取得では読み取り数・レスポンス
 * サイズが際限なく膨らんで最終的に一覧そのものが失敗する。技術設計書10章の非機能要件が
 * 一覧系APIにページネーションを前提としていることにも合わせる（他の一覧APIと同じ`before`/`limit`）。
 *
 * ブロック関係（双方向）にあるユーザーの投稿を除外する（技術設計書5-2章・12-2-3章）。除外は取得後に
 * メモリ上で行うため、ページ内の件数が`limit`より少なくなることがある（クライアントは件数ではなく
 * 「`limit`件取得できたか」で次ページの有無を判断する）。
 */
export async function listBoardPosts(
  requesterUserId: string,
  params: ListBoardPostsParams = {}
): Promise<BoardPostResponse[]> {
  let query: Query = db.collection("boardPosts").orderBy("createdAt", "desc");
  query = applyBeforeCursor(query, params.before);
  query = query.limit(parseLimit(params.limit));

  const snap = await query.get();
  const posts = snap.docs.map((d) => d.data() as BoardPostDoc);
  const visiblePosts = await excludeBlockedUsers(posts, requesterUserId, (p) => p.userId);
  return visiblePosts.map(toBoardPostResponse);
}

export interface CreateBoardPostInput {
  content: string;
}

/** `POST /board`（技術設計書6-6章）。 */
export async function createBoardPost(userId: string, input: CreateBoardPostInput): Promise<BoardPostResponse> {
  const postId = newId();
  const now = Timestamp.now();
  const doc: BoardPostDoc = {
    postId,
    userId,
    content: input.content,
    createdAt: now,
  };
  await db.collection("boardPosts").doc(postId).set(doc);
  return toBoardPostResponse(doc);
}
