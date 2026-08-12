import * as logger from "firebase-functions/logger";
import { Twilio } from "twilio";
import { AppError } from "../../../lib/AppError";
import type { SmsSender } from "./SmsSender";

/**
 * クライアントに返すメッセージ（技術設計書12-6章）。
 * Twilio側の内部エラー詳細・設定不備の内容は含めず、詳細はCloud Loggingにのみ出力する。
 */
const SEND_FAILED_MESSAGE = "確認コードの送信に失敗しました。時間をおいて再度お試しください";

/**
 * Twilioを用いたSMS送信の実装（技術設計書12-5章、ADR-0003・ADR-0008）。
 *
 * 認証情報（`TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_FROM_NUMBER`）は`process.env`から読み込む。
 * デプロイ環境ではCloud Functions v2のシークレット（`src/index.ts`の`defineSecret` + `secrets: [...]`）が
 * 実行時に`process.env`へ注入され、ローカルでは`functions/.env`が同じ変数名で読み込まれる（README参照）。
 *
 * ログに関する制約（本番用実装のため`ConsoleSmsSender`とは方針が異なる）:
 * - 本文（`body`＝OTPコードを含む）は一切ログ出力しない
 * - 認証情報は値をログ出力しない（未設定時に「どの環境変数が未設定か」の変数名のみ出力する）
 * - 宛先電話番号は下4桁のみに伏せて出力する
 * - Twilio SDKの例外オブジェクトはそのまま出力せず（HTTPクライアントの例外はAuthorizationヘッダ等を
 *   保持しうるため）、status / code / message のみを抽出して出力する
 */
export class TwilioSmsSender implements SmsSender {
  private client: Twilio | null = null;
  private fromNumber: string | null = null;

  async send(phoneNumber: string, body: string): Promise<void> {
    const { client, fromNumber } = this.resolveClient();

    try {
      await client.messages.create({ to: phoneNumber, from: fromNumber, body });
    } catch (err) {
      logger.error(
        `[TwilioSmsSender] SMS送信に失敗しました 宛先=${maskPhoneNumber(phoneNumber)} ${describeTwilioError(err)}`
      );
      throw new AppError(500, "INTERNAL", SEND_FAILED_MESSAGE);
    }
  }

  /** Twilioクライアントを遅延生成する（初回送信時に認証情報を読み込み、以降はインスタンス内にキャッシュする）。 */
  private resolveClient(): { client: Twilio; fromNumber: string } {
    if (this.client && this.fromNumber) {
      return { client: this.client, fromNumber: this.fromNumber };
    }

    const accountSid = process.env.TWILIO_ACCOUNT_SID;
    const authToken = process.env.TWILIO_AUTH_TOKEN;
    const fromNumber = process.env.TWILIO_FROM_NUMBER;

    if (!accountSid || !authToken || !fromNumber) {
      // 値そのものは出力せず、未設定の環境変数名のみを出力する
      const missing = [
        ["TWILIO_ACCOUNT_SID", accountSid],
        ["TWILIO_AUTH_TOKEN", authToken],
        ["TWILIO_FROM_NUMBER", fromNumber],
      ]
        .filter(([, value]) => !value)
        .map(([name]) => name);
      logger.error(
        `[TwilioSmsSender] Twilioの認証情報が未設定のためSMSを送信できません（未設定の環境変数: ${missing.join(", ")}）。` +
          "デプロイ環境では`firebase functions:secrets:set <名前>`で、ローカルでは`functions/.env`で設定してください" +
          "（functions/README.md、技術設計書12-5章）。"
      );
      throw new AppError(500, "INTERNAL", SEND_FAILED_MESSAGE);
    }

    try {
      this.client = new Twilio(accountSid, authToken);
    } catch (err) {
      logger.error(`[TwilioSmsSender] Twilioクライアントの初期化に失敗しました ${describeTwilioError(err)}`);
      throw new AppError(500, "INTERNAL", SEND_FAILED_MESSAGE);
    }
    this.fromNumber = fromNumber;
    return { client: this.client, fromNumber };
  }
}

/** 宛先電話番号を下4桁のみに伏せる（ログにPIIをそのまま残さないため）。 */
function maskPhoneNumber(phoneNumber: string): string {
  return `***${phoneNumber.slice(-4)}`;
}

/** 例外から診断に必要な最小限の情報（status / code / message）のみを抽出する。 */
function describeTwilioError(err: unknown): string {
  if (typeof err === "object" && err !== null) {
    const e = err as { status?: unknown; code?: unknown; message?: unknown };
    const parts: string[] = [];
    if (e.status !== undefined) parts.push(`status=${String(e.status)}`);
    if (e.code !== undefined) parts.push(`code=${String(e.code)}`);
    if (typeof e.message === "string") parts.push(`message=${e.message}`);
    if (parts.length > 0) return parts.join(" ");
  }
  return "詳細不明のエラー";
}
