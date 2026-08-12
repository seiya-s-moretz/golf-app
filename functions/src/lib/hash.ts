import { createHash } from "node:crypto";

/**
 * SHA-256ハッシュを16進文字列で返す。
 * アクセストークン（ADR-0008）・OTPコード（5-2章PhoneVerification）・電話番号（phoneVerificationsの
 * ドキュメントID）のハッシュ化で共通利用する。
 */
export function sha256Hex(input: string): string {
  return createHash("sha256").update(input, "utf8").digest("hex");
}
