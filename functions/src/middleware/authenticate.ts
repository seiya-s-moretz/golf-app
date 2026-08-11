import type { NextFunction, Request, Response } from "express";
import { db } from "../config/firebaseAdmin";
import { AppError } from "../lib/AppError";
import { sha256Hex } from "../lib/hash";
import type { SessionDoc, UserDoc } from "../types/firestore";

/**
 * Bearerトークン認証ミドルウェア（技術設計書12-3章・12-4章、ADR-0008）。
 *
 * 1. `Authorization: Bearer <token>`ヘッダーを取得。無ければ401
 * 2. `sha256(token)`で`sessions/{tokenHash}`をGet。存在しない、または`expiresAt`が過去なら401
 * 3. `sessions.userId`から`users/{userId}`をGet。存在しない、または`status===SUSPENDED`なら401
 *    （凍結アカウントは次のリクエストから即座にアクセス不可になる）
 * 4. 解決した`User`を`req.currentUser`にセットして次へ
 *
 * `POST /auth/phone/otp`, `POST /auth/phone/verify`, `POST /users`, `GET /areas` には適用しない。
 */
export async function authenticate(req: Request, _res: Response, next: NextFunction): Promise<void> {
  try {
    const token = extractBearerToken(req.header("authorization"));
    if (!token) {
      throw new AppError(401, "UNAUTHENTICATED", "Authorizationヘッダーが必要です");
    }

    const sessionSnap = await db.collection("sessions").doc(sha256Hex(token)).get();
    if (!sessionSnap.exists) {
      throw new AppError(401, "UNAUTHENTICATED", "無効なアクセストークンです");
    }
    const session = sessionSnap.data() as SessionDoc;
    if (session.expiresAt.toMillis() < Date.now()) {
      throw new AppError(401, "UNAUTHENTICATED", "アクセストークンの有効期限が切れています");
    }

    const userSnap = await db.collection("users").doc(session.userId).get();
    if (!userSnap.exists) {
      throw new AppError(401, "UNAUTHENTICATED", "ユーザーが見つかりません");
    }
    const userDoc = userSnap.data() as UserDoc;
    if (userDoc.status === "SUSPENDED") {
      throw new AppError(401, "UNAUTHENTICATED", "アカウントが凍結されています");
    }

    req.currentUser = {
      userId: userDoc.userId,
      isAdmin: userDoc.isAdmin,
      status: userDoc.status,
      doc: userDoc,
    };
    next();
  } catch (err) {
    next(err);
  }
}

function extractBearerToken(header: string | undefined): string | null {
  if (!header) return null;
  const match = /^Bearer\s+(.+)$/i.exec(header.trim());
  return match ? match[1].trim() : null;
}
