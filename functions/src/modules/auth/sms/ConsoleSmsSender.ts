import * as logger from "firebase-functions/logger";
import type { SmsSender } from "./SmsSender";

/**
 * ダミー実装（技術設計書12-5章）。ローカル開発・Firebase Emulator Suiteでの動作確認用。
 * 送信内容（宛先電話番号・本文＝OTPコード）をログ出力するのみで、実際のSMS送信は行わない。
 */
export class ConsoleSmsSender implements SmsSender {
  async send(phoneNumber: string, body: string): Promise<void> {
    logger.info(`[ConsoleSmsSender] SMS宛先=${phoneNumber} 本文="${body}"`);
  }
}
