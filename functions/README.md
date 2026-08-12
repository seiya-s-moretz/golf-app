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

## Twilio（実SMS送信）の設定

`POST /auth/phone/otp`のSMS送信実装は`SmsSender`インターフェースの背後で切り替わる（技術設計書12-5章）。

| 環境 | 選択される実装 |
|---|---|
| Emulator実行時（`FUNCTIONS_EMULATOR=true`が自動設定される） | `ConsoleSmsSender`（ログ出力のみ） |
| デプロイ環境 | `TwilioSmsSender`（実送信） |
| `SMS_PROVIDER=console` / `twilio` を明示した場合 | 指定した実装（上記より優先） |

### デプロイ環境（Secret Manager）

`TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_FROM_NUMBER`はCloud Functions v2のシークレットとして
管理する（`src/index.ts`で`defineSecret()`し、`api`関数に`secrets: [...]`でバインド済み。バインドしないと
実行時に`process.env`へ注入されない）。デプロイ前に以下を実行してSecret Managerへ値を登録する。

```
cd functions
npx firebase functions:secrets:set TWILIO_ACCOUNT_SID   # 実行後、値の入力を求められる
npx firebase functions:secrets:set TWILIO_AUTH_TOKEN
npx firebase functions:secrets:set TWILIO_FROM_NUMBER   # E.164形式（例: +15551234567）

npm run deploy
```

補足:
- 登録済みシークレットの確認は`npx firebase functions:secrets:access TWILIO_ACCOUNT_SID`、一覧は
  `npx firebase functions:secrets:get`。値を更新した場合は再デプロイで新しいバージョンが関数に反映される
- シークレット未登録の状態でも**コードのロード・ビルド・テストは失敗しない**（`defineSecret()`はロード時に
  Secret Managerへアクセスしないため）。ただし`firebase deploy`はバインド対象のシークレットが存在しないと
  エラーになるため、初回デプロイ前に必ず上記3つを登録すること
- 実行時に認証情報が未設定だった場合、`TwilioSmsSender`は「どの環境変数が未設定か」をCloud Loggingに出力し、
  クライアントには`500 / INTERNAL`（汎用メッセージ）を返す

### ローカルで実SMS送信を試す場合

`functions/.env.example`を`functions/.env`にコピーし（`.env`はgit管理外）、Twilioの実値と
`SMS_PROVIDER=twilio`を設定してからEmulatorを起動する。実際にSMSが送信され課金される点に注意。

```
cp .env.example .env   # 値を編集し SMS_PROVIDER=twilio を設定
npm run serve
```

### ログの取り扱い

`TwilioSmsSender`は本番用実装のため、**SMS本文（OTPコードを含む）を一切ログ出力しない**。認証情報も値は
出力せず（未設定時に変数名のみ出力）、宛先電話番号は下4桁のみに伏せる。送信失敗時もTwilio SDKの例外
オブジェクトをそのまま出力せず、`status` / `code` / `message`のみを抽出して出力する。
一方`ConsoleSmsSender`は開発用途のため、OTPコードを含む本文をそのまま出力する。

## 自動テスト（TesterAgentが導入）

Jest + supertest + Firestore Emulatorによる統合テストを`test/`配下に整備している（`src/`配下のテストコードはない。理由は下記の通りモジュール構成上`test/`に分離している）。単体モック中心のテストではなく、実Firestore（Emulator）に対してExpressアプリ（`createApp()`）をsupertest経由で叩く統合テストとして実装した（12-1章の「モック差し替えのためのリポジトリインターフェースを用意する必要性が薄い」「実データストアに対してテストするほうが本番との乖離を防げる」という設計方針に沿う）。

```
cd functions
npm test
```

`npm test`は内部で`firebase emulators:exec --only firestore ... "jest --runInBand"`を実行し、Firestore Emulatorを一時起動した状態でテストスイートを実行する（テスト前後でEmulatorの起動・終了までCLIが面倒を見るため、事前にEmulatorを手動起動しておく必要はない）。

補足:
- `firebase-tools`は`^13.35.1`をdevDependencyとして固定している。理由: 2026-08時点の最新版（`firebase-tools@15`系）はFirestore EmulatorがJava 21以上を要求するが、開発機のJavaは17系のため、Java 17でも動作する`13.x`系を採用した（`npm run serve`等の既存スクリプトが参照するグローバル/npx実行の`firebase-tools`とはバージョンが異なる可能性がある点に注意。ローカルEmulator起動でJavaバージョンエラーが出た場合は`npx --package=firebase-tools@13.35.1 firebase ...`のように明示指定するか、Java 21以上を導入すること）
- 例外的に`test/auth/twilioSmsSender.test.ts`のみ統合テストではなく軽量なユニットテスト（Firestore不使用）。実Twilioアカウントを前提にできないため、「認証情報未設定時に500/INTERNALを返す」「ログにOTPコード・認証情報が残らない」ことのみを検証する
- OTPコードはSMS送信のダミー実装（`ConsoleSmsSender`）が`firebase-functions/logger`経由でログ出力する内容をテストコード側で捕捉して読み取っている（`test/setup/consoleCapture.ts`）。ロガーの出力形式（`auth.service.ts`内の文言）が変わった場合は追随修正が必要
- 既知の環境依存の癖（Windows）: Firestore Emulatorの子プロセス（`java.exe`）が、CLI側が「正常終了」をログ出力した後もOSプロセスとして残留し、次回のテスト実行時に「Port 8080 is not open」エラーで失敗することがある（`firebase-tools`側のシグナルハンドリングに起因すると見られる既知の環境依存の癖で、本プロジェクトのバグではない）。発生した場合は`netstat -ano | findstr :8080`等でプロセスを特定し終了させてから再実行すること

## ディレクトリ構成

`src/modules/<feature>/`配下に`routes.ts`（HTTPの受付）＋`service.ts`（バリデーション後のビジネスロジック＋
Firestoreアクセス）の2層のみを置く。Android側のようなrepository/usecaseインターフェース層は設けない
（ADR-0008参照）。

## 実装フェーズ

Phase1: 認証基盤（OTP・Bearerトークン）、エリアマスタ、ユーザープロフィール、
ラウンド募集（申請→承認フロー、Connection生成含む）。実装済み。

Phase2（本コミット時点で実装済み）: おすすめユーザー（`GET /users/recommend`、要件定義書3-1章のスコアリング
ロジック適用）、マッチング申請（作成・一覧・承認/却下、Connection生成含む）、掲示板（一覧・投稿）。
ブロック関係の除外フィルタはPhase3で追加する（技術設計書13-3章の依存関係、現時点では未適用）。

Phase3（メッセージ・通報・ブロック・通報管理）は未実装（技術設計書13章）。
