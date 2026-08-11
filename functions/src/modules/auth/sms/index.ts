import { ConsoleSmsSender } from "./ConsoleSmsSender";
import type { SmsSender } from "./SmsSender";
import { TwilioSmsSender } from "./TwilioSmsSender";

export type { SmsSender } from "./SmsSender";

let cachedSender: SmsSender | null = null;

/**
 * 実装の選択（技術設計書12-5章、ADR-0008）。
 * Firebase Emulator Suiteは環境変数`FUNCTIONS_EMULATOR=true`を自動設定するため、これを検知した場合は
 * 自動的に`ConsoleSmsSender`を返す。加えて明示的な上書き用に`SMS_PROVIDER=console|twilio`も用意する。
 */
export function getSmsSender(): SmsSender {
  if (cachedSender) return cachedSender;

  const provider = process.env.SMS_PROVIDER;
  const isEmulator = process.env.FUNCTIONS_EMULATOR === "true";

  if (provider === "twilio") {
    cachedSender = new TwilioSmsSender();
  } else if (provider === "console") {
    cachedSender = new ConsoleSmsSender();
  } else if (isEmulator) {
    cachedSender = new ConsoleSmsSender();
  } else {
    cachedSender = new TwilioSmsSender();
  }

  return cachedSender;
}
