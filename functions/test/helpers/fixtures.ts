import type { Express } from "express";
import request from "supertest";
import { Timestamp } from "firebase-admin/firestore";
import { randomUUID } from "node:crypto";
import { db } from "../../src/config/firebaseAdmin";
import { extractLatestOtpCode } from "../setup/consoleCapture";

let phoneSeq = 0;

/** テストごとに一意なE.164形式の電話番号を発行する。 */
export function nextPhoneNumber(): string {
  phoneSeq += 1;
  return `+8190${String(10000000 + phoneSeq).padStart(8, "0")}`;
}

export interface SeedAreaOverrides {
  areaId?: string;
  prefecture?: string;
  areaName?: string;
  displayOrder?: number;
  isActive?: boolean;
}

/** `areaMasters`に1件投入する（技術設計書5-2章AreaMaster）。DBへ直接書き込む（GET /areasを経由しない）。 */
export async function seedArea(overrides: SeedAreaOverrides = {}): Promise<string> {
  const areaId = overrides.areaId ?? randomUUID();
  await db
    .collection("areaMasters")
    .doc(areaId)
    .set({
      areaId,
      prefecture: overrides.prefecture ?? "東京都",
      areaName: overrides.areaName ?? "テストエリア",
      displayOrder: overrides.displayOrder ?? 1,
      isActive: overrides.isActive ?? true,
      createdAt: Timestamp.now(),
    });
  return areaId;
}

export interface RegisterNewUserOverrides {
  phoneNumber?: string;
  areaId?: string;
  name?: string;
  gender?: string;
  age?: number;
  averageScore?: number;
  purpose?: "CASUAL" | "SERIOUS" | "LESSON_WANTED";
  introduction?: string;
}

export interface RegisteredUser {
  phoneNumber: string;
  userId: string;
  accessToken: string;
  areaId: string;
  // 生のAPIレスポンスボディ（User、technical design 6-3章のUserResponse形）
  user: Record<string, unknown>;
}

/**
 * `POST /auth/phone/otp` → `POST /auth/phone/verify` → `POST /users` の一連のフローを
 * 実際にHTTP経由で叩き、新規ユーザーを1名登録してaccess_tokenを取得するテストヘルパー。
 * ADR-0003・ADR-0006・技術設計書6-1章のとおりの3段階フローをそのままなぞることで、
 * 個別モジュールのテスト（round-events等）で「認証済みユーザーが必要」なケースの前提を作る。
 */
export async function registerNewUser(app: Express, overrides: RegisterNewUserOverrides = {}): Promise<RegisteredUser> {
  const phoneNumber = overrides.phoneNumber ?? nextPhoneNumber();
  const areaId = overrides.areaId ?? (await seedArea());

  await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(204);
  const otpCode = extractLatestOtpCode(phoneNumber);

  const verifyRes = await request(app)
    .post("/auth/phone/verify")
    .send({ phone_number: phoneNumber, otp_code: otpCode })
    .expect(200);
  if (verifyRes.body.is_new_user !== true) {
    throw new Error("registerNewUser: 新規ユーザーとして判定されませんでした（テストヘルパーの前提が崩れています）");
  }

  const registerRes = await request(app)
    .post("/users")
    .send({
      registration_token: verifyRes.body.registration_token,
      name: overrides.name ?? "テスト太郎",
      gender: overrides.gender ?? "MALE",
      age: overrides.age ?? 30,
      area_id: areaId,
      average_score: overrides.averageScore ?? 100,
      purpose: overrides.purpose ?? "CASUAL",
      introduction: overrides.introduction ?? "よろしくお願いします",
    })
    .expect(201);

  return {
    phoneNumber,
    areaId,
    userId: registerRes.body.user.user_id,
    accessToken: registerRes.body.access_token,
    user: registerRes.body.user,
  };
}

/** `Authorization: Bearer <token>` ヘッダーを付与するsupertestの補助。 */
export function authHeader(accessToken: string): [string, string] {
  return ["Authorization", `Bearer ${accessToken}`];
}

/**
 * `User.isAdmin`をDBへ直接書き込むテストヘルパー（ADR-0007のとおり`is_admin`付与APIはMVPでは存在しないため、
 * 管理者アカウントを用意するにはDB直接操作しかない。運用上の想定と同じ操作をテストでも行う）。
 */
export async function setAdmin(userId: string, isAdmin = true): Promise<void> {
  await db.collection("users").doc(userId).update({ isAdmin });
}

/**
 * `POST /users/{id}/match-requests` → `POST /match-requests/{id}/approve` の一連のHTTPフローを実際に叩き、
 * 2ユーザー間に`Connection`を成立させるテストヘルパー（メッセージ機能のテスト前提を作るため）。
 */
export async function establishConnection(app: Express, userA: RegisteredUser, userB: RegisteredUser): Promise<void> {
  const created = await request(app)
    .post(`/users/${userB.userId}/match-requests`)
    .set(...authHeader(userA.accessToken))
    .expect(201);
  await request(app)
    .post(`/match-requests/${created.body.match_request_id}/approve`)
    .set(...authHeader(userB.accessToken))
    .expect(200);
}
