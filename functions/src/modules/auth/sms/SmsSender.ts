/**
 * SMS送信の抽象化インターフェース（技術設計書12-5章、ADR-0008）。
 * `auth.service.ts`はこのインターフェースにのみ依存し、具体実装（Twilio/Console）を知らない。
 */
export interface SmsSender {
  send(phoneNumber: string, body: string): Promise<void>;
}
