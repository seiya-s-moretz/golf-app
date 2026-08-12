import type { Timestamp } from "firebase-admin/firestore";

/**
 * Firestoreドキュメントの型定義（技術設計書12-2章）。
 *
 * 5章の各Entity定義は「APIレスポンスとして返る論理データモデル」であり、Firestoreの実ドキュメントが
 * それと1対1である必要はない（技術設計書12-0章 前提1）。ここではPhase1で使用するコレクションのみ定義する
 * （Phase2/3のコレクション型は各フェーズ実装時に追加する）。
 */

// ---- users/{userId} ----

export type UserStatus = "ACTIVE" | "SUSPENDED";

/** 目的タグ（PRD 0章。Androidクライアント`domain/model/User.kt`のPurpose enumのwire値と一致させる）。 */
export type Purpose = "CASUAL" | "SERIOUS" | "LESSON_WANTED";

export interface UserDoc {
  userId: string;
  name: string;
  iconUrl: string;
  gender: string;
  age: number;
  areaId: string;
  averageScore: number;
  purpose: Purpose;
  introduction: string;
  phoneNumber: string;
  phoneVerified: boolean;
  phoneVerifiedAt: Timestamp | null;
  status: UserStatus;
  isAdmin: boolean;
  createdAt: Timestamp;
}

// ---- areaMasters/{areaId} ----

export interface AreaMasterDoc {
  areaId: string;
  prefecture: string;
  areaName: string;
  displayOrder: number;
  isActive: boolean;
  createdAt: Timestamp;
}

// ---- phoneVerifications/{sha256(phoneNumber)} ----

export type PhoneVerificationStatus = "PENDING" | "VERIFIED" | "EXPIRED" | "FAILED";

export interface PhoneVerificationDoc {
  verificationId: string;
  phoneNumber: string;
  otpCodeHash: string;
  status: PhoneVerificationStatus;
  expiresAt: Timestamp;
  attemptCount: number;
  createdAt: Timestamp;
  verifiedAt: Timestamp | null;
  /**
   * `POST /users`の本登録に用いる`registration_token`のハッシュ値（内部専用フィールド）。
   *
   * 5章のPhoneVerification Entity定義には無いフィールドであり、12-2-2章のコレクション一覧にも
   * `registration_token`専用の新規コレクションは定義されていない。本実装では「新規コレクションを
   * 増やさず、OTP検証と同じ電話番号キーで管理できるphoneVerificationsドキュメントを内部拡張する」
   * という判断を行った（12-2章 前提1: 内部専用フィールドの追加は5章定義の「変更」にあたらない、を根拠とする）。
   * OTP検証成功時に「新規ユーザー」と判定された場合にのみ値が設定され、`POST /users`での消費時にnullへ戻す
   * （1回限りの使い切りトークンとして扱う）。この設計判断はDeveloperAgentの実装時判断であり、
   * ArchitectAgentによる正式なレビューは未実施（実装メモ参照）。
   */
  registrationTokenHash: string | null;
  registrationTokenExpiresAt: Timestamp | null;
}

// ---- sessions/{sha256(rawToken)} ----

export interface SessionDoc {
  userId: string;
  createdAt: Timestamp;
  expiresAt: Timestamp;
}

// ---- roundEvents/{eventId} ----

export interface RoundEventDoc {
  eventId: string;
  clubName: string;
  datetime: string;
  fee: number;
  capacity: number;
  current: number;
  createdBy: string;
  createdAt: Timestamp;
}

// ---- roundEvents/{eventId}/joinRequests/{requestId} ----

export type RoundJoinRequestStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface RoundJoinRequestDoc {
  joinRequestId: string;
  eventId: string;
  userId: string;
  status: RoundJoinRequestStatus;
  createdAt: Timestamp;
  respondedAt: Timestamp | null;
}

// ---- connections/{pairId} ----

export type ConnectionSourceType = "MATCH_REQUEST" | "ROUND_JOIN";

export interface ConnectionDoc {
  connectionId: string;
  userAId: string;
  userBId: string;
  sourceType: ConnectionSourceType;
  sourceId: string;
  createdAt: Timestamp;
  /** 会話プレビュー用の非正規化フィールド（Phase3のメッセージ機能が書き込む。技術設計書12-2-3章）。 */
  lastMessageAt?: Timestamp;
  lastMessagePreview?: string;
  unreadCountForUserA?: number;
  unreadCountForUserB?: number;
}

// ---- matchRequests/{matchRequestId} ----

export type MatchRequestStatus = "PENDING" | "ACCEPTED" | "REJECTED";

export interface MatchRequestDoc {
  matchRequestId: string;
  fromUserId: string;
  toUserId: string;
  status: MatchRequestStatus;
  createdAt: Timestamp;
  respondedAt: Timestamp | null;
}

// ---- boardPosts/{postId} ----

export interface BoardPostDoc {
  postId: string;
  userId: string;
  content: string;
  createdAt: Timestamp;
}
