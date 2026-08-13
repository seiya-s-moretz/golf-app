import { Timestamp } from "firebase-admin/firestore";
import { db } from "../../config/firebaseAdmin";
import { AppError } from "../../lib/AppError";
import { assertValidDocumentId } from "../../lib/documentId";
import { newId } from "../../lib/ids";
import { applyBeforeCursor, parseLimit } from "../../lib/pagination";
import { buildPairId, normalizePair } from "../../lib/pairId";
import { assertNotBlocked, excludeBlockedUsers } from "../blocks/blocks.service";
import { getConnection } from "../connections/connections.service";
import { toUserResponse, type UserResponse } from "../users/users.service";
import type { ConnectionDoc, MessageDoc, UserDoc } from "../../types/firestore";

/**
 * メッセージモジュール（技術設計書6-7章）。
 *
 * ディレクトリ名は技術設計書12-1章の構成（`modules/messaging/`）に合わせている
 * （DeveloperAgentへの指示文では`modules/messages/`という表記もあったが、12章の正式なディレクトリ構成を優先した）。
 */

export interface MessageResponse {
  message_id: string;
  user_a_id: string;
  user_b_id: string;
  sender_id: string;
  content: string;
  created_at: string;
  read_at: string | null;
}

export interface ConversationResponse {
  partner: UserResponse;
  last_message: MessageResponse | null;
  unread_count: number;
  updated_at: string;
}

function toMessageResponse(doc: MessageDoc): MessageResponse {
  return {
    message_id: doc.messageId,
    user_a_id: doc.userAId,
    user_b_id: doc.userBId,
    sender_id: doc.senderId,
    content: doc.content,
    created_at: doc.createdAt.toDate().toISOString(),
    read_at: doc.readAt ? doc.readAt.toDate().toISOString() : null,
  };
}

/**
 * `GET /conversations`（技術設計書6-7章）。自分が関わる会話一覧（Connectionが存在するユーザーペア単位）を
 * 最終更新日時（最新メッセージ送信日時。メッセージが1件も無い場合はConnection作成日時）降順で返す。
 * `Connection`への非正規化フィールド（技術設計書12-2-3章）を使い、Connection数に比例した追加クエリを避ける。
 */
export async function listConversations(userId: string): Promise<ConversationResponse[]> {
  const [asUserA, asUserB] = await Promise.all([
    db.collection("connections").where("userAId", "==", userId).get(),
    db.collection("connections").where("userBId", "==", userId).get(),
  ]);
  const allConnectionDocs = [...asUserA.docs, ...asUserB.docs].map((d) => d.data() as ConnectionDoc);
  // ブロック関係にある相手との会話は一覧から除外する（他の一覧系APIと同じ扱い。技術設計書5-2章）。
  // 除外しないと、送信すると403になる会話がプレビュー付きで残り続け、閉じる手段が無い
  const connectionDocs = await excludeBlockedUsers(allConnectionDocs, userId, (c) =>
    c.userAId === userId ? c.userBId : c.userAId
  );
  if (connectionDocs.length === 0) return [];

  const partnerIds = connectionDocs.map((c) => (c.userAId === userId ? c.userBId : c.userAId));
  const partnerSnaps = await db.getAll(...partnerIds.map((id) => db.collection("users").doc(id)));
  const partnerDocById = new Map(
    partnerSnaps.filter((s) => s.exists).map((s) => [s.id, s.data() as UserDoc])
  );

  const conversations = await Promise.all(
    connectionDocs.map(async (connection): Promise<ConversationResponse | null> => {
      const partnerId = connection.userAId === userId ? connection.userBId : connection.userAId;
      const partnerDoc = partnerDocById.get(partnerId);
      // 相手ユーザーが何らかの事情で存在しない（データ不整合）場合は一覧から除外する
      if (!partnerDoc) return null;

      const isUserA = connection.userAId === userId;
      const unreadCount = (isUserA ? connection.unreadCountForUserA : connection.unreadCountForUserB) ?? 0;
      const updatedAt = connection.lastMessageAt ?? connection.createdAt;
      const lastMessage: MessageResponse | null =
        connection.lastMessageId && connection.lastMessageAt
          ? {
              message_id: connection.lastMessageId,
              user_a_id: connection.userAId,
              user_b_id: connection.userBId,
              sender_id: connection.lastMessageSenderId ?? "",
              content: connection.lastMessagePreview ?? "",
              created_at: connection.lastMessageAt.toDate().toISOString(),
              read_at: connection.lastMessageReadAt ? connection.lastMessageReadAt.toDate().toISOString() : null,
            }
          : null;

      return {
        partner: await toUserResponse(partnerDoc, userId),
        last_message: lastMessage,
        unread_count: unreadCount,
        updated_at: updatedAt.toDate().toISOString(),
      };
    })
  );

  return conversations
    .filter((c): c is ConversationResponse => c !== null)
    .sort((a, b) => new Date(b.updated_at).getTime() - new Date(a.updated_at).getTime());
}

/**
 * `GET /conversations/{partnerId}/messages`（技術設計書6-7章）。
 * `Connection`が存在しない場合は403。ページネーションは`before`/`limit`（技術設計書12-1章の共通ヘルパー）。
 */
export async function listMessages(
  requesterUserId: string,
  partnerId: string,
  before: string | undefined,
  limitRaw: unknown
): Promise<MessageResponse[]> {
  assertValidDocumentId(partnerId, "partnerId");
  const connection = await getConnection(requesterUserId, partnerId);
  if (!connection) {
    throw new AppError(403, "FORBIDDEN", "このユーザーとの会話は存在しません");
  }

  const pairId = buildPairId(requesterUserId, partnerId);
  let query = db.collection("messages").where("pairId", "==", pairId).orderBy("createdAt", "desc");
  query = applyBeforeCursor(query, before);
  query = query.limit(parseLimit(limitRaw));

  const snap = await query.get();
  return snap.docs.map((d) => toMessageResponse(d.data() as MessageDoc));
}

/**
 * `POST /conversations/{partnerId}/messages`（技術設計書6-7章）。
 * `Connection`が存在しない、またはブロック関係にある場合は403。
 * `content`の空文字・最大文字数超過チェックはルーティング層のzodバリデーションで行う。
 *
 * Firestoreトランザクション内で(1)Connection存在チェック・ブロック関係チェック、
 * (2)`messages`への新規作成、(3)`connections`の会話プレビュー用非正規化フィールド更新を同時に行う
 * （技術設計書12-2-3章）。
 */
export async function sendMessage(
  requesterUserId: string,
  partnerId: string,
  content: string
): Promise<MessageResponse> {
  assertValidDocumentId(partnerId, "partnerId");
  if (requesterUserId === partnerId) {
    throw new AppError(400, "VALIDATION_ERROR", "自分自身にはメッセージを送信できません");
  }
  const partnerSnap = await db.collection("users").doc(partnerId).get();
  if (!partnerSnap.exists) {
    throw new AppError(404, "NOT_FOUND", "相手ユーザーが見つかりません");
  }

  const pairId = buildPairId(requesterUserId, partnerId);
  const { userAId, userBId } = normalizePair(requesterUserId, partnerId);
  const connectionRef = db.collection("connections").doc(pairId);
  const messageId = newId();
  const messageRef = db.collection("messages").doc(messageId);

  const created = await db.runTransaction(async (tx) => {
    const connectionSnap = await tx.get(connectionRef);
    if (!connectionSnap.exists) {
      throw new AppError(403, "FORBIDDEN", "このユーザーとのメッセージは許可されていません");
    }
    // Firestoreトランザクションは全ての読み取りを全ての書き込みより先に行う必要があるため、
    // ブロック関係チェック（追加のGet）もここで完了させる。
    await assertNotBlocked(requesterUserId, partnerId, tx);

    const connection = connectionSnap.data() as ConnectionDoc;
    const now = Timestamp.now();
    const doc: MessageDoc = {
      messageId,
      pairId,
      userAId,
      userBId,
      senderId: requesterUserId,
      content,
      createdAt: now,
      readAt: null,
    };
    tx.create(messageRef, doc);

    const isSenderA = requesterUserId === userAId;
    const currentUnreadForRecipient = isSenderA
      ? connection.unreadCountForUserB ?? 0
      : connection.unreadCountForUserA ?? 0;
    tx.update(connectionRef, {
      lastMessageAt: now,
      lastMessagePreview: content,
      lastMessageId: messageId,
      lastMessageSenderId: requesterUserId,
      lastMessageReadAt: null,
      ...(isSenderA
        ? { unreadCountForUserB: currentUnreadForRecipient + 1 }
        : { unreadCountForUserA: currentUnreadForRecipient + 1 }),
    });

    return doc;
  });

  return toMessageResponse(created);
}

/**
 * `POST /conversations/{partnerId}/read`（技術設計書6-7章）。相手からの未読メッセージの`read_at`を更新する。
 * `Connection`が存在しない場合は他のconversations系APIと同様403とする（6章に明記は無いが一貫性のための
 * DeveloperAgent判断）。
 */
export async function markConversationRead(requesterUserId: string, partnerId: string): Promise<void> {
  assertValidDocumentId(partnerId, "partnerId");
  const pairId = buildPairId(requesterUserId, partnerId);
  const { userAId } = normalizePair(requesterUserId, partnerId);
  const connectionRef = db.collection("connections").doc(pairId);
  const connectionSnap = await connectionRef.get();
  if (!connectionSnap.exists) {
    throw new AppError(403, "FORBIDDEN", "このユーザーとの会話は存在しません");
  }
  const isRequesterA = requesterUserId === userAId;

  // `pairId`単一フィールドの等価条件のみで取得し（自動インデックスのみで足りる）、
  // 相手からの未読メッセージの絞り込みはアプリケーションコード側で行う（MVP規模のメッセージ量を前提とした
  // 実装判断。技術設計書12-2-3章のブロック除外フィルタと同じ考え方で、複合インデックスの追加を避ける）。
  const messagesSnap = await db.collection("messages").where("pairId", "==", pairId).get();
  const unreadFromPartner = messagesSnap.docs.filter((d) => {
    const data = d.data() as MessageDoc;
    return data.senderId === partnerId && data.readAt === null;
  });
  const markedMessageIds = new Set(unreadFromPartner.map((d) => (d.data() as MessageDoc).messageId));

  const now = Timestamp.now();
  const BATCH_CHUNK_SIZE = 400; // Firestoreバッチの上限500件に対し余裕を持たせる
  for (let i = 0; i < unreadFromPartner.length; i += BATCH_CHUNK_SIZE) {
    const batch = db.batch();
    unreadFromPartner.slice(i, i + BATCH_CHUNK_SIZE).forEach((d) => {
      batch.update(d.ref, { readAt: now });
    });
    await batch.commit();
  }

  // 未読件数は「0にする」のではなく「既読にした件数だけ減らす」。既読化の最中に相手から新しい
  // メッセージが届くと、0固定では**その新着が未読として二度と数えられない**（一覧の未読バッジは
  // `Connection`の非正規化カウンタしか見ておらず、再計算する箇所が無い）。
  // `lastMessageReadAt`も、実際に既読化したメッセージが最新メッセージだった場合にのみ更新する
  // （古いスナップショットを信じると、直前に届いた未読メッセージを既読として表示してしまう）。
  await db.runTransaction(async (tx) => {
    const latestSnap = await tx.get(connectionRef);
    if (!latestSnap.exists) return;
    const latest = latestSnap.data() as ConnectionDoc;

    const currentUnread = (isRequesterA ? latest.unreadCountForUserA : latest.unreadCountForUserB) ?? 0;
    const nextUnread = Math.max(0, currentUnread - markedMessageIds.size);

    const connectionUpdate: Record<string, unknown> = isRequesterA
      ? { unreadCountForUserA: nextUnread }
      : { unreadCountForUserB: nextUnread };
    if (latest.lastMessageId && markedMessageIds.has(latest.lastMessageId)) {
      connectionUpdate.lastMessageReadAt = now;
    }
    tx.update(connectionRef, connectionUpdate);
  });
}
