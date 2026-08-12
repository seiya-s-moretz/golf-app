import { Timestamp } from "firebase-admin/firestore";
import request from "supertest";
import { db } from "../../src/config/firebaseAdmin";
import { sha256Hex } from "../../src/lib/hash";
import { createApp } from "../../src/app";
import { extractLatestOtpCode } from "../setup/consoleCapture";
import { nextPhoneNumber, registerNewUser } from "../helpers/fixtures";

/**
 * `POST /auth/phone/verify`（技術設計書6-1章、ADR-0006）。
 * OTP不一致で400、正しいOTPで`is_new_user`が新規/既存で正しく分岐することを検証する。
 */
describe("POST /auth/phone/verify", () => {
  const app = createApp();

  test("OTPを一度も発行していない電話番号でverifyすると400 VALIDATION_ERRORを返す", async () => {
    const res = await request(app)
      .post("/auth/phone/verify")
      .send({ phone_number: nextPhoneNumber(), otp_code: "123456" })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("OTPコードが不一致の場合は400 VALIDATION_ERRORを返す", async () => {
    const phoneNumber = nextPhoneNumber();
    await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(204);
    const correctOtp = extractLatestOtpCode(phoneNumber);
    const wrongOtp = correctOtp === "000000" ? "111111" : "000000";

    const res = await request(app)
      .post("/auth/phone/verify")
      .send({ phone_number: phoneNumber, otp_code: wrongOtp })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("otp_codeが6桁数字でない場合はzodバリデーションにより400を返す", async () => {
    const res = await request(app)
      .post("/auth/phone/verify")
      .send({ phone_number: nextPhoneNumber(), otp_code: "abc" })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("未登録の電話番号を正しいOTPで検証するとis_new_user=trueかつregistration_tokenが返る", async () => {
    const phoneNumber = nextPhoneNumber();
    await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(204);
    const otpCode = extractLatestOtpCode(phoneNumber);

    const res = await request(app)
      .post("/auth/phone/verify")
      .send({ phone_number: phoneNumber, otp_code: otpCode })
      .expect(200);

    expect(res.body.is_new_user).toBe(true);
    expect(typeof res.body.registration_token).toBe("string");
    expect(res.body.registration_token.length).toBeGreaterThan(0);
    expect(res.body.user).toBeUndefined();
    expect(res.body.access_token).toBeUndefined();
  });

  test("登録済みの電話番号を正しいOTPで検証するとis_new_user=falseかつsession.user・session.access_tokenが返る（再ログイン、ADR-0006）", async () => {
    const registered = await registerNewUser(app);

    // registerNewUser内で直近にOTPを発行済みのため、60秒レート制限（otp.test.ts参照）に抵触しないよう
    // phoneVerificationsのcreatedAtを直接過去に書き換えてから再ログイン用のOTPを発行する。
    const verificationRef = db.collection("phoneVerifications").doc(sha256Hex(registered.phoneNumber));
    const verificationSnap = await verificationRef.get();
    await verificationRef.update({
      createdAt: Timestamp.fromMillis(verificationSnap.data()!.createdAt.toMillis() - 61_000),
    });

    // 同じ電話番号で再度OTPを発行・検証する（再ログインフロー）
    await request(app).post("/auth/phone/otp").send({ phone_number: registered.phoneNumber }).expect(204);
    const otpCode = extractLatestOtpCode(registered.phoneNumber);

    const res = await request(app)
      .post("/auth/phone/verify")
      .send({ phone_number: registered.phoneNumber, otp_code: otpCode })
      .expect(200);

    expect(res.body.is_new_user).toBe(false);
    expect(res.body.session.user.user_id).toBe(registered.userId);
    expect(typeof res.body.session.access_token).toBe("string");
    expect(res.body.registration_token).toBeUndefined();
  });

  /**
   * 【修正確認】is_new_user=false時、user・access_tokenがADR-0006どおりsessionにネストされて返る。
   *
   * ADR-0006「実装への影響」表（AuthDto.kt行）は、`VerifyOtpResponseDto`が
   * `session: AuthSessionResponseDto?`（`is_new_user=false`時のみ非null。`AuthSessionResponseDto`は
   * `user`・`access_token`を持つネストしたオブジェクト）を持つと明記しており、実際
   * `app/src/main/java/com/golfmatch/app/data/dto/AuthDto.kt`もそのとおり実装されている
   * （`VerifyOtpResponseDto(is_new_user, session: AuthSessionResponseDto?, registration_token)`）。
   * `AuthMapper.kt`の`VerifyOtpResponseDto.toDomain()`は`is_new_user=false`時に
   * `checkNotNull(session)`でsessionフィールドの存在を必須としている。
   *
   * 以前は`user`・`access_token`が`session`にネストされずトップレベルのフィールドとして返っており
   * （`docs/test-plan.md` 6-4-1章参照）、Androidクライアント側で`checkNotNull(session)`が例外を
   * 送出する重大バグだった。`verifyPhoneOtp()`の修正により`session`にネストされるようになったことを
   * 確認する。
   */
  test("is_new_user=false時、user・access_tokenがADR-0006どおりsessionにネストされて返る（Androidクライアント契約準拠）", async () => {
    const registered = await registerNewUser(app);
    const verificationRef = db.collection("phoneVerifications").doc(sha256Hex(registered.phoneNumber));
    const verificationSnap = await verificationRef.get();
    await verificationRef.update({
      createdAt: Timestamp.fromMillis(verificationSnap.data()!.createdAt.toMillis() - 61_000),
    });
    await request(app).post("/auth/phone/otp").send({ phone_number: registered.phoneNumber }).expect(204);
    const otpCode = extractLatestOtpCode(registered.phoneNumber);

    const res = await request(app)
      .post("/auth/phone/verify")
      .send({ phone_number: registered.phoneNumber, otp_code: otpCode })
      .expect(200);

    // ADR-0006・AuthDto.ktが期待する構造
    expect(res.body.session).toBeDefined();
    expect(res.body.session.user.user_id).toBe(registered.userId);
    expect(typeof res.body.session.access_token).toBe("string");
    // トップレベルには user・access_token を置かない
    expect(res.body.user).toBeUndefined();
    expect(res.body.access_token).toBeUndefined();
  });

  test("試行回数の上限（5回）を超えて不一致OTPを送るとFAILED状態になりその後は正しいOTPでも400を返す", async () => {
    const phoneNumber = nextPhoneNumber();
    await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(204);
    const correctOtp = extractLatestOtpCode(phoneNumber);
    const wrongOtp = correctOtp === "000000" ? "111111" : "000000";

    for (let i = 0; i < 5; i += 1) {
      await request(app)
        .post("/auth/phone/verify")
        .send({ phone_number: phoneNumber, otp_code: wrongOtp })
        .expect(400);
    }

    // 上限到達後は正しいOTPコードでも失敗する
    const res = await request(app)
      .post("/auth/phone/verify")
      .send({ phone_number: phoneNumber, otp_code: correctOtp })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("有効期限切れのOTPを検証すると400 VALIDATION_ERRORを返す（境界値、Firestore Emulator上のexpiresAtを直接過去に書き換えて再現）", async () => {
    const phoneNumber = nextPhoneNumber();
    await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(204);
    const otpCode = extractLatestOtpCode(phoneNumber);

    const ref = db.collection("phoneVerifications").doc(sha256Hex(phoneNumber));
    await ref.update({ expiresAt: Timestamp.fromMillis(Date.now() - 1_000) });

    const res = await request(app)
      .post("/auth/phone/verify")
      .send({ phone_number: phoneNumber, otp_code: otpCode })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });
});
