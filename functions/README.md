# functions（Cloud Functions for Firebase）

ゴルフマッチングアプリのサーバーサイド実装（TypeScript / Express / Firestore）。設計は
`docs/技術設計書.md` 12章・13章、`docs/adr/0008-server-implementation-design.md`を参照。

## セットアップ

```
cd functions
npm install
```

## ローカル動作確認（Firebase Emulator Suite）

Firebase CLIが未インストールの場合は `npm install -g firebase-tools` するか、`npx firebase-tools` で代用できる。

```
# 1. ビルド + Emulator起動（Functions / Firestore / Auth）
npm run serve

# 2. 別ターミナルでエリアマスタのシードデータを投入（GET /areas・POST /users の動作確認に必須）
FIRESTORE_EMULATOR_HOST=localhost:8080 npm run seed:areas
```

Emulator起動後、`http://localhost:5001/<projectId>/asia-northeast1/api/` 配下に技術設計書6章のAPIパスが
そのままマウントされる（例: `POST http://localhost:5001/golf-app-dev-placeholder/asia-northeast1/api/auth/phone/otp`）。
`<projectId>`は`.firebaserc`の値。

OTPコードは`ConsoleSmsSender`によりFunctionsエミュレータのログ（ターミナル出力）に出力される
（技術設計書12-8章）。実SMS送信は行わない。

## 自動テスト（TesterAgentが導入）

Jest + supertest + Firestore Emulatorによる統合テストを`test/`配下に整備している（`src/`配下のテストコードはない。理由は下記の通りモジュール構成上`test/`に分離している）。単体モック中心のテストではなく、実Firestore（Emulator）に対してExpressアプリ（`createApp()`）をsupertest経由で叩く統合テストとして実装した（12-1章の「モック差し替えのためのリポジトリインターフェースを用意する必要性が薄い」「実データストアに対してテストするほうが本番との乖離を防げる」という設計方針に沿う）。

```
cd functions
npm test
```

`npm test`は内部で`firebase emulators:exec --only firestore ... "jest --runInBand"`を実行し、Firestore Emulatorを一時起動した状態でテストスイートを実行する（テスト前後でEmulatorの起動・終了までCLIが面倒を見るため、事前にEmulatorを手動起動しておく必要はない）。

補足:
- `firebase-tools`は`^13.35.1`をdevDependencyとして固定している。理由: 2026-08時点の最新版（`firebase-tools@15`系）はFirestore EmulatorがJava 21以上を要求するが、開発機のJavaは17系のため、Java 17でも動作する`13.x`系を採用した（`npm run serve`等の既存スクリプトが参照するグローバル/npx実行の`firebase-tools`とはバージョンが異なる可能性がある点に注意。ローカルEmulator起動でJavaバージョンエラーが出た場合は`npx --package=firebase-tools@13.35.1 firebase ...`のように明示指定するか、Java 21以上を導入すること）
- OTPコードはSMS送信のダミー実装（`ConsoleSmsSender`）が`firebase-functions/logger`経由でログ出力する内容をテストコード側で捕捉して読み取っている（`test/setup/consoleCapture.ts`）。ロガーの出力形式（`auth.service.ts`内の文言）が変わった場合は追随修正が必要
- 既知の環境依存の癖（Windows）: Firestore Emulatorの子プロセス（`java.exe`）が、CLI側が「正常終了」をログ出力した後もOSプロセスとして残留し、次回のテスト実行時に「Port 8080 is not open」エラーで失敗することがある（`firebase-tools`側のシグナルハンドリングに起因すると見られる既知の環境依存の癖で、本プロジェクトのバグではない）。発生した場合は`netstat -ano | findstr :8080`等でプロセスを特定し終了させてから再実行すること

## ディレクトリ構成

`src/modules/<feature>/`配下に`routes.ts`（HTTPの受付）＋`service.ts`（バリデーション後のビジネスロジック＋
Firestoreアクセス）の2層のみを置く。Android側のようなrepository/usecaseインターフェース層は設けない
（ADR-0008参照）。

## 実装フェーズ

Phase1（本コミット時点）: 認証基盤（OTP・Bearerトークン）、エリアマスタ、ユーザープロフィール、
ラウンド募集（申請→承認フロー、Connection生成含む）。Phase2（おすすめユーザー・マッチング申請・掲示板）、
Phase3（メッセージ・通報・ブロック・通報管理）は未実装（技術設計書13章）。
