import { Timestamp } from "firebase-admin/firestore";
import { db } from "../../config/firebaseAdmin";
import { newId } from "../../lib/ids";
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

/**
 * `GET /board`（技術設計書6-6章）。`created_at`降順で全件返す（6章にページネーションの明記が無く、
 * Androidクライアント`ApiService.getBoardPosts()`もクエリパラメータを取らないため、単純な全件取得とした）。
 * ブロック関係（双方向）にあるユーザーの投稿を除外する（Phase3で追加。技術設計書5-2章・12-2-3章）。
 */
export async function listBoardPosts(requesterUserId: string): Promise<BoardPostResponse[]> {
  const snap = await db.collection("boardPosts").orderBy("createdAt", "desc").get();
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
