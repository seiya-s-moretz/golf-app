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

## ディレクトリ構成

`src/modules/<feature>/`配下に`routes.ts`（HTTPの受付）＋`service.ts`（バリデーション後のビジネスロジック＋
Firestoreアクセス）の2層のみを置く。Android側のようなrepository/usecaseインターフェース層は設けない
（ADR-0008参照）。

## 実装フェーズ

Phase1（本コミット時点）: 認証基盤（OTP・Bearerトークン）、エリアマスタ、ユーザープロフィール、
ラウンド募集（申請→承認フロー、Connection生成含む）。Phase2（おすすめユーザー・マッチング申請・掲示板）、
Phase3（メッセージ・通報・ブロック・通報管理）は未実装（技術設計書13章）。
