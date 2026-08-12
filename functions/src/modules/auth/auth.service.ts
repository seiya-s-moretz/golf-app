import { randomBytes } from "node:crypto";
import { Timestamp } from "firebase-admin/firestore";
import { db } from "../../config/firebaseAdmin";
import { AppError } from "../../lib/AppError";
import { sha256Hex } from "../../lib/hash";
import { newId } from "../../lib/ids";
import { getAreaById } from "../areas/areas.service";
import { toUserResponse, type UserResponse } from "../users/users.service";
import { getSmsSender } from "./sms";
import type { PhoneVerificationDoc, Purpose, UserDoc } from "../../types/firestore";

/**
 * OTP発行・検証、新規/既存ユーザー判定、セッション発行のロジック（技術設計書12-1章）。
 * ADR-0003・ADR-0006（`POST /auth/phone/verify`への新規/既存ユーザー判定統合）・ADR-0008
 * （不透明トークン＋Firestoreセッションストア）に基づく。
 */

// OTP有効期限（技術設計書5-2章「発行から5分程度を想定」に準拠）
const OTP_TTL_MS = 5 * 60 * 1000;
// 再送信の最短間隔（技術設計書6-1章「例: 60秒」に準拠）
const OTP_RESEND_INTERVAL_MS = 60 * 1000;
// OTP試行回数の上限（5-2章「規定回数超過でFAILEDにしブルートフォース対策」。具体的な回数は未指定のため実装判断で5回とする）
const OTP_MAX_ATTEMPTS = 5;
// registration_tokenの有効期限（6-1章「短期有効」とのみ規定。具体的な値は未指定のため実装判断で30分とする）
const REGISTRATION_TOKEN_TTL_MS = 30 * 60 * 1000;
// アクセストークンの有効期限（技術設計書12-3章、ADR-0008「90日」に準拠）
const SESSION_TTL_MS = 90 * 24 * 60 * 60 * 1000;

function phoneVerificationRef(phoneNumber: string) {
  return db.collection("phoneVerifications").doc(sha256Hex(phoneNumber));
}

function generateOtpCode(): string {
  return String(Math.floor(100000 + Math.random() * 900000));
}

/** `POST /auth/phone/otp`（技術設計書6-1章）。 */
export async function requestPhoneOtp(phoneNumber: string): Promise<void> {
  const ref = phoneVerificationRef(phoneNumber);
  const existing = await ref.get();
  const now = Timestamp.now();

  if (existing.exists) {
    const data = existing.data() as PhoneVerificationDoc;
    const elapsedMs = now.toMillis() - data.createdAt.toMillis();
    if (elapsedMs < OTP_RESEND_INTERVAL_MS) {
      throw new AppError(429, "RATE_LIMITED", "確認コードの再送信は60秒以上間隔を空けてください");
    }
  }

  const otpCode = generateOtpCode();
  const doc: PhoneVerificationDoc = {
    verificationId: newId(),
    phoneNumber,
    otpCodeHash: sha256Hex(otpCode),
    status: "PENDING",
    expiresAt: Timestamp.fromMillis(now.toMillis() + OTP_TTL_MS),
    attemptCount: 0,
    createdAt: now,
    verifiedAt: null,
    registrationTokenHash: null,
    registrationTokenExpiresAt: null,
  };
  // 電話番号ごとに1ドキュメントとし、再送信のたびに上書きする（過去のOTPは無効化される。技術設計書12-2-2章）
  await ref.set(doc);

  await getSmsSender().send(phoneNumber, `【ゴルフマッチング】確認コード: ${otpCode}（5分間有効）`);
}

export interface VerifyPhoneOtpSession {
  user: UserResponse;
  access_token: string;
}

export interface VerifyPhoneOtpResult {
  is_new_user: boolean;
  session?: VerifyPhoneOtpSession;
  registration_token?: string;
}

/** `POST /auth/phone/verify`（技術設計書6-1章、ADR-0006）。 */
export async function verifyPhoneOtp(phoneNumber: string, otpCode: string): Promise<VerifyPhoneOtpResult> {
  const ref = phoneVerificationRef(phoneNumber);
  const snap = await ref.get();
  if (!snap.exists) {
    throw new AppError(400, "VALIDATION_ERROR", "確認コードが発行されていません。最初から操作をやり直してください");
  }
  const data = snap.data() as PhoneVerificationDoc;
  const now = Timestamp.now();

  if (data.status === "FAILED") {
    throw new AppError(400, "VALIDATION_ERROR", "試行回数の上限を超えました。確認コードを再送信してください");
  }
  if (data.expiresAt.toMillis() < now.toMillis()) {
    await ref.update({ status: "EXPIRED" });
    throw new AppError(400, "VALIDATION_ERROR", "確認コードの有効期限が切れています。再送信してください");
  }

  if (sha256Hex(otpCode) !== data.otpCodeHash) {
    const attemptCount = data.attemptCount + 1;
    if (attemptCount >= OTP_MAX_ATTEMPTS) {
      await ref.update({ attemptCount, status: "FAILED" });
    } else {
      await ref.update({ attemptCount });
    }
    throw new AppError(400, "VALIDATION_ERROR", "確認コードが正しくありません");
  }

  await ref.update({ status: "VERIFIED", verifiedAt: now });

  const existingUserSnap = await db
    .collection("users")
    .where("phoneNumber", "==", phoneNumber)
    .where("phoneVerified", "==", true)
    .limit(1)
    .get();

  if (!existingUserSnap.empty) {
    const userDoc = existingUserSnap.docs[0].data() as UserDoc;
    const accessToken = await createSession(userDoc.userId);
    return {
      is_new_user: false,
      session: {
        user: await toUserResponse(userDoc),
        access_token: accessToken,
      },
    };
  }

  const registrationToken = randomBytes(32).toString("base64url");
  await ref.update({
    registrationTokenHash: sha256Hex(registrationToken),
    registrationTokenExpiresAt: Timestamp.fromMillis(now.toMillis() + REGISTRATION_TOKEN_TTL_MS),
  });

  return { is_new_user: true, registration_token: registrationToken };
}

export interface RegisterUserInput {
  registration_token: string;
  name: string;
  gender: string;
  age: number;
  area_id: string;
  average_score: number;
  purpose: Purpose;
  introduction: string;
}

export interface RegisterUserResult {
  user: UserResponse;
  access_token: string;
}

/** `POST /users`（新規登録。技術設計書6-1章）。 */
export async function registerUser(input: RegisterUserInput): Promise<RegisterUserResult> {
  const tokenHash = sha256Hex(input.registration_token);
  const querySnap = await db
    .collection("phoneVerifications")
    .where("registrationTokenHash", "==", tokenHash)
    .limit(1)
    .get();

  if (querySnap.empty) {
    throw new AppError(401, "UNAUTHENTICATED", "registration_tokenが無効です");
  }
  const verificationDocRef = querySnap.docs[0].ref;
  const verification = querySnap.docs[0].data() as PhoneVerificationDoc;
  const now = Timestamp.now();

  if (
    verification.status !== "VERIFIED" ||
    !verification.registrationTokenExpiresAt ||
    verification.registrationTokenExpiresAt.toMillis() < now.toMillis()
  ) {
    throw new AppError(401, "UNAUTHENTICATED", "registration_tokenの有効期限が切れています");
  }

  const existingUserSnap = await db
    .collection("users")
    .where("phoneNumber", "==", verification.phoneNumber)
    .where("phoneVerified", "==", true)
    .limit(1)
    .get();
  if (!existingUserSnap.empty) {
    throw new AppError(409, "CONFLICT", "この電話番号は既に登録済みです");
  }

  const area = await getAreaById(input.area_id);
  if (!area || !area.isActive) {
    throw new AppError(400, "VALIDATION_ERROR", "指定されたエリアは選択できません");
  }

  const userId = newId();
  const userDoc: UserDoc = {
    userId,
    name: input.name,
    iconUrl: "",
    gender: input.gender,
    age: input.age,
    areaId: input.area_id,
    averageScore: input.average_score,
    purpose: input.purpose,
    introduction: input.introduction,
    phoneNumber: verification.phoneNumber,
    phoneVerified: true,
    phoneVerifiedAt: now,
    status: "ACTIVE",
    isAdmin: false,
    createdAt: now,
  };
  await db.collection("users").doc(userId).set(userDoc);

  // registration_tokenを使い切りにする（再利用防止）
  await verificationDocRef.update({ registrationTokenHash: null, registrationTokenExpiresAt: null });

  const accessToken = await createSession(userId);

  return { user: await toUserResponse(userDoc), access_token: accessToken };
}

/** アクセストークンを発行し`sessions`に保存する（技術設計書12-3章、ADR-0008）。 */
async function createSession(userId: string): Promise<string> {
  const rawToken = randomBytes(32).toString("base64url");
  const now = Timestamp.now();
  await db
    .collection("sessions")
    .doc(sha256Hex(rawToken))
    .set({
      userId,
      createdAt: now,
      expiresAt: Timestamp.fromMillis(now.toMillis() + SESSION_TTL_MS),
    });
  return rawToken;
}
