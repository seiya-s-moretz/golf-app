import { Timestamp } from "firebase-admin/firestore";
import request from "supertest";
import { db } from "../../src/config/firebaseAdmin";
import { sha256Hex } from "../../src/lib/hash";
import { createApp } from "../../src/app";
import { extractLatestOtpCode } from "../setup/consoleCapture";
import { nextPhoneNumber } from "../helpers/fixtures";

/**
 * `POST /auth/phone/otp`（技術設計書6-1章）。
 * - パラメータ: phone_number
 * - 認証: 不要
 * - バリデーション: 同一電話番号への再送信は60秒以上間隔を空ける（429）
 */
describe("POST /auth/phone/otp", () => {
  const app = createApp();

  test("有効なE.164形式の電話番号でOTP送信要求は204を返す", async () => {
    const phoneNumber = nextPhoneNumber();
    await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(204);
  });

  test("E.164形式でない電話番号は400 VALIDATION_ERRORを返す", async () => {
    const res = await request(app).post("/auth/phone/otp").send({ phone_number: "09012345678" }).expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("phone_numberが未指定の場合は400 VALIDATION_ERRORを返す", async () => {
    const res = await request(app).post("/auth/phone/otp").send({}).expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("同一電話番号へ60秒以内に再送信すると429 RATE_LIMITEDを返す", async () => {
    const phoneNumber = nextPhoneNumber();
    await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(204);

    const res = await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(429);
    expect(res.body.error.code).toBe("RATE_LIMITED");
  });

  test("60秒以内の再送信リクエストは元のOTPコードを上書きしない（旧OTPが引き続き有効）", async () => {
    const phoneNumber = nextPhoneNumber();
    await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(204);
    const originalOtp = extractLatestOtpCode(phoneNumber);

    await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(429);

    const verifyRes = await request(app)
      .post("/auth/phone/verify")
      .send({ phone_number: phoneNumber, otp_code: originalOtp })
      .expect(200);
    expect(verifyRes.body.is_new_user).toBe(true);
  });

  test("60秒経過後の再送信は204で受理され新しいOTPコードが発行される", async () => {
    const phoneNumber = nextPhoneNumber();
    await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(204);
    const originalOtp = extractLatestOtpCode(phoneNumber);

    // 実時間で60秒待つ代わりに、Firestore Emulator上のcreatedAtを直接過去日時に書き換えて
    // 「60秒経過後」の状態を再現する（境界値テストを高速に行うための手法。src側の実装は変更しない）。
    const ref = db.collection("phoneVerifications").doc(sha256Hex(phoneNumber));
    const snap = await ref.get();
    const data = snap.data()!;
    await ref.update({ createdAt: Timestamp.fromMillis(data.createdAt.toMillis() - 61_000) });

    await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(204);
    const newOtp = extractLatestOtpCode(phoneNumber);
    expect(newOtp).not.toBe(originalOtp);

    // 古いOTPコードはもう有効ではない（新しいコードで上書きされたため）
    const verifyOldRes = await request(app)
      .post("/auth/phone/verify")
      .send({ phone_number: phoneNumber, otp_code: originalOtp })
      .expect(400);
    expect(verifyOldRes.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("異なる電話番号への送信は互いの60秒レート制限に影響しない", async () => {
    const phoneA = nextPhoneNumber();
    const phoneB = nextPhoneNumber();
    await request(app).post("/auth/phone/otp").send({ phone_number: phoneA }).expect(204);
    await request(app).post("/auth/phone/otp").send({ phone_number: phoneB }).expect(204);
  });
});
