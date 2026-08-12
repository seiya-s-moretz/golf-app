/**
 * Jest設定（TesterAgentが導入）。
 *
 * Firestore Emulatorに対する統合テストを想定するため、`npm test`は
 * `firebase-tools emulators:exec --only firestore "jest"`経由で実行し、
 * FIRESTORE_EMULATOR_HOST等の環境変数がFirebase CLIによって自動的に注入された状態でJestを起動する
 * （functions/README.md・package.json参照）。単体でjestコマンドのみを叩く場合は
 * 別途Firestore Emulatorを起動しFIRESTORE_EMULATOR_HOSTを手動設定すること。
 */
module.exports = {
  preset: "ts-jest",
  testEnvironment: "node",
  rootDir: ".",
  roots: ["<rootDir>/test"],
  testMatch: ["**/*.test.ts"],
  // env.ts: FIRESTORE_EMULATOR_HOSTチェック・SMS_PROVIDER強制（テストフレームワーク初期化前に実行する必要がある）
  // consoleCapture.ts: firebase-functions/loggerがconsole.infoの参照を固定的に保持する前にフックする必要がある
  setupFiles: ["<rootDir>/test/setup/env.ts", "<rootDir>/test/setup/consoleCapture.ts"],
  // afterEnv.ts: beforeEach()等のテストフレームワークAPIを使うためsetupFilesAfterEnvで登録する
  setupFilesAfterEnv: ["<rootDir>/test/setup/afterEnv.ts"],
  globals: {
    "ts-jest": {
      tsconfig: "tsconfig.jest.json",
    },
  },
  testTimeout: 30000,
  // 複数テストファイルが同じFirestore Emulatorインスタンスを共有するため、
  // データ競合を避けて逐次実行する（emulators:exec側でも--runInBandを指定しているが二重の安全策）。
  maxWorkers: 1,
};
