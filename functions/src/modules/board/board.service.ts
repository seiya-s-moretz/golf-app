import { Timestamp } from "firebase-admin/firestore";
import { db } from "../../config/firebaseAdmin";
import { newId } from "../../lib/ids";
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

/**
 * `GET /board`（技術設計書6-6章）。`created_at`降順で全件返す（6章にページネーションの明記が無く、
 * Androidクライアント`ApiService.getBoardPosts()`もクエリパラメータを取らないため、単純な全件取得とした）。
 * ブロックしたユーザーの投稿を除外するフィルタはPhase3で追加する（技術設計書13-3章の依存関係）。
 */
export async function listBoardPosts(): Promise<BoardPostResponse[]> {
  const snap = await db.collection("boardPosts").orderBy("createdAt", "desc").get();
  return snap.docs.map((d) => toBoardPostResponse(d.data() as BoardPostDoc));
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
