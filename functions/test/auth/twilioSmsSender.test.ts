import * as logger from "firebase-functions/logger";
import { AppError } from "../../src/lib/AppError";
import { TwilioSmsSender } from "../../src/modules/auth/sms/TwilioSmsSender";

/**
 * `TwilioSmsSender`の軽量ユニットテスト（技術設計書12-5章）。
 *
 * 実Twilioアカウントを持たないため実送信経路は検証せず、「認証情報未設定時の挙動」と
 * 「ログに秘匿情報（OTPコード本文・認証情報）が残らないこと」のみを対象とする。
 */
describe("TwilioSmsSender", () => {
  const ENV_KEYS = ["TWILIO_ACCOUNT_SID", "TWILIO_AUTH_TOKEN", "TWILIO_FROM_NUMBER"] as const;
  const saved: Record<string, string | undefined> = {};

  beforeEach(() => {
    for (const key of ENV_KEYS) {
      saved[key] = process.env[key];
      delete process.env[key];
    }
  });

  afterEach(() => {
    for (const key of ENV_KEYS) {
      if (saved[key] === undefined) delete process.env[key];
      else process.env[key] = saved[key];
    }
    jest.restoreAllMocks();
  });

  it("認証情報が未設定の場合、500/INTERNALのAppErrorを投げる（内部詳細はレスポンス用メッセージに含めない）", async () => {
    jest.spyOn(logger, "error").mockImplementation(() => undefined);

    const error = await new TwilioSmsSender().send("+819012345678", "【ゴルフマッチング】確認コード: 123456（5分間有効）").then(
      () => null,
      (e: unknown) => e
    );

    expect(error).toBeInstanceOf(AppError);
    const appError = error as AppError;
    expect(appError.httpStatus).toBe(500);
    expect(appError.code).toBe("INTERNAL");
    expect(appError.message).toBe("確認コードの送信に失敗しました。時間をおいて再度お試しください");
    expect(appError.message).not.toContain("TWILIO_");
  });

  it("認証情報未設定時のログには未設定の環境変数名のみを出力し、OTPコード本文・宛先全体は出力しない", async () => {
    const errorLog = jest.spyOn(logger, "error").mockImplementation(() => undefined);
    process.env.TWILIO_ACCOUNT_SID = "ACdummy";

    await expect(
      new TwilioSmsSender().send("+819012345678", "【ゴルフマッチング】確認コード: 123456（5分間有効）")
    ).rejects.toBeInstanceOf(AppError);

    expect(errorLog).toHaveBeenCalledTimes(1);
    const logged = String(errorLog.mock.calls[0][0]);
    expect(logged).toContain("TWILIO_AUTH_TOKEN");
    expect(logged).toContain("TWILIO_FROM_NUMBER");
    // 設定済みの変数名・値、およびOTPコード本文はログに含まれない
    expect(logged).not.toContain("ACdummy");
    expect(logged).not.toContain("123456");
  });
});
