/**
 * Jestの環境変数セットアップ（`setupFiles`、テストフレームワーク初期化前に実行）。
 *
 * `SMS_PROVIDER=console`を明示することで、`functions/src/modules/auth/sms/index.ts`の
 * `getSmsSender()`が`FUNCTIONS_EMULATOR`環境変数の有無に関わらず必ず`ConsoleSmsSender`を返すようにする
 * （このテストはFunctions Emulatorを起動せずExpressアプリを直接importして検証するため、
 * `FUNCTIONS_EMULATOR=true`は自動設定されない。プロダクションコード側の分岐条件（技術設計書12-5章）を
 * 変更せず、既存の`SMS_PROVIDER`明示オーバーライド経路を利用するだけなので`functions/src`への変更は不要）。
 */
process.env.SMS_PROVIDER = "console";

if (!process.env.GCLOUD_PROJECT) {
  process.env.GCLOUD_PROJECT = "golf-app-dev-placeholder";
}

if (!process.env.FIRESTORE_EMULATOR_HOST) {
  // eslint-disable-next-line no-console
  console.error(
    "FIRESTORE_EMULATOR_HOST が設定されていません。`npm test`（Firestore Emulator経由）で実行してください。" +
      "詳細はfunctions/README.mdまたはdocs/test-plan.mdを参照。"
  );
  process.exit(1);
}
