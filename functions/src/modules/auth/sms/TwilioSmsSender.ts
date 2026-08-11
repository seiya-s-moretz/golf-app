import type { SmsSender } from "./SmsSender";

/**
 * Twilioを用いたSMS送信の実装（技術設計書12-5章、ADR-0003・ADR-0008）。
 *
 * 実Twilioアカウントは本エンゲージメントでは未用意のため（10章参照）、今回のスコープでは
 * 実際に呼び出されることはない（`getSmsSender()`はEmulator実行時に自動的に`ConsoleSmsSender`を返す）。
 * `twilio` npmパッケージも未導入のためスタブ実装に留める。
 *
 * 有効化する手順:
 *   1. `npm install twilio` を実行し依存関係に追加する
 *   2. `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_FROM_NUMBER` を
 *      Cloud Functionsのシークレット（`defineSecret`）またはローカル`.env`で設定する
 *   3. 下記`send()`の実装をTwilio SDK呼び出しに置き換える
 */
export class TwilioSmsSender implements SmsSender {
  async send(_phoneNumber: string, _body: string): Promise<void> {
    const accountSid = process.env.TWILIO_ACCOUNT_SID;
    const authToken = process.env.TWILIO_AUTH_TOKEN;
    const fromNumber = process.env.TWILIO_FROM_NUMBER;
    if (!accountSid || !authToken || !fromNumber) {
      throw new Error(
        "TwilioSmsSender: TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN / TWILIO_FROM_NUMBER が未設定です。" +
          "実Twilioアカウント確保後に設定してください（技術設計書12-5章）。"
      );
    }

    // 実アカウント未用意のため twilio SDK は依存関係に追加していない。有効化時は以下のように実装する。
    //
    //   const twilioClient = require("twilio")(accountSid, authToken);
    //   await twilioClient.messages.create({ to: _phoneNumber, from: fromNumber, body: _body });
    throw new Error("TwilioSmsSenderは未実装です。Twilioアカウント確保後に実装してください（技術設計書12-5章）。");
  }
}
