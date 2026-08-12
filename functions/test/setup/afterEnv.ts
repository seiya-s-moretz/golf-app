/**
 * `setupFilesAfterEnv`: テストフレームワーク（`beforeEach`等）が使える状態で実行される共通セットアップ。
 * 各テスト間でFirestore Emulatorのデータとログキャプチャバッファをクリアし、テスト間の状態漏れを防ぐ。
 */
import { clearFirestoreEmulator } from "../helpers/firestoreEmulator";
import { clearCapturedLogs } from "./consoleCapture";

beforeEach(async () => {
  await clearFirestoreEmulator();
  clearCapturedLogs();
});
