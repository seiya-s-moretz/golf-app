# ADR-0005: `POST /auth/login` レスポンスに `User` を含める

## ステータス
承認（ただし本ADRが対象とした `POST /auth/login` エンドポイント自体は、ADR-0006により `POST /auth/phone/verify` に統合され廃止された。本ADRが定めた「レスポンスに`User`を含める」「暗黙のnullフォールバックを禁止する」という原則は、統合後の `POST /auth/phone/verify` の既存ユーザー分岐にそのまま引き継がれており、本ADRの決定内容自体は無効化しない）

## コンテキスト
TesterAgentによる検証（`docs/test-plan.md` 4-1章）で、以下の不整合が発見された。

- 技術設計書（旧版）6-1章では、`POST /auth/login` のレスポンスは `access_token` のみと定義されていた。一方 `POST /users`（新規登録）のレスポンスは「作成された`User`、`access_token`」であり、両者で非対称な定義になっていた。
- 実装（`AuthSessionResponseDto`）は `POST /users` と `POST /auth/login` の両方で同一のレスポンスDTO（`user: UserDto?`, `accessToken: String`）を共用しており、`user` はnullable。
- `domain.AuthSession.userId` は非null `String` であり、`AuthMapper.toDomain()` は `user?.userId.orEmpty()` という実装で、`user` が存在しない場合は空文字列にサイレントにフォールバックしていた。
- 設計どおり「ログインレスポンスに`user`を含めない」を維持すると、ログイン成功のたびに`userId`が常に空文字列になる。これは以降のAPI呼び出し（`GET /users/{id}`等、本人のuser_idを要求する画面）に誤ったIDを渡す、またはユーザー情報の取得に失敗するという実害につながる。「ログインできたのに自分が誰か分からない」状態はアプリの基本機能が成立しないバグであり、単なるnull安全性の問題にとどまらない。

対応案として以下3案が提示された。

- 案A: `POST /auth/login` レスポンスにも `user` を含めるよう技術設計書6-1章を見直す
- 案B: `AuthRepositoryImpl.login()` 内で追加の `GET /users/me` 相当のAPIを呼び出し `userId` を解決する
- 案C: 暗黙の空文字列フォールバックをやめ、`AuthSession.userId` をnullable化するか例外を投げる

## 決定
**案Aを採用し、案Cの一部（暗黙フォールバックの廃止・明示的な失敗）を組み合わせる。案Bは不採用とする。**

### 1. `POST /auth/login` のレスポンスに `User` を含める（技術設計書6-1章を修正）
`POST /auth/login` は認証成功時点で対象ユーザーのレコードをサーバーが特定済みであり、そのユーザー情報を返すことに追加のデータアクセスコストはほぼ無い。`POST /users`（新規登録）と同じレスポンス形（`User` + `access_token`）に統一することで、

- クライアントは2つの認証系エンドポイント（登録・ログイン）を同一の`AuthSessionResponseDto`・同一の`toDomain()`ロジックで扱える（実装は既にこの前提でDTOを共用していた。設計書側がこの実装意図に追従していなかったのが本来の不整合の原因と判断する）
- ログイン直後から`userId`はもちろん、名前・アイコン等の表示に使うプロフィール情報も即座に得られ、ログイン後にプロフィール取得のための追加API呼び出しが不要になる
- API設計全体の一貫性が保たれる（`POST /users`と`POST /auth/login`は「認証してセッションを開始する」という同じ性質のAPIであり、レスポンス形が異なる理由がそもそもない）

### 2. `AuthMapper`の暗黙フォールバックを廃止する
`user`が万一欠落した場合に`userId = ""`へサイレントに倒れる実装は、たとえレスポンス契約上`user`が必須になったとしても、契約違反を握りつぶし後続処理に誤ったデータを流すリスクがあるアンチパターンである。契約違反時は早期に検知できるよう、`AuthMapper.toDomain()`は`user`が`null`の場合に明示的な例外を送出する実装に変更する（`AuthSession.userId`自体は非null `String`のまま維持し、nullable化はしない。契約上必須の値をドメインモデル側で「無いかもしれない」ものとして表現するのは実態と合わず、呼び出し側に不要なnullハンドリングを強いるため）。

## 代替案（検討したが不採用）
### 案B: ログイン後に `GET /users/me` 相当のAPIを追加呼び出しする
- 不採用理由: (1) 新規APIエンドポイントの追加が必要になり、既存の`GET /users/{id}`との役割重複（`/me`という「自分自身」を指すエイリアスの導入是非）を新たに設計する必要が生じる、(2) ログイン成功から`userId`確定までの間に余分なネットワークラウンドトリップが挟まり、失敗時のハンドリング（トークンは取れたがユーザー情報取得に失敗、という中間状態）が複雑になる、(3) サーバーは認証時点で対象ユーザーを既に特定しているため、素直にレスポンスに含めれば済む話であり、往復を増やす合理性がない。実装のシンプルさの観点で案Aに劣る。

### 案C単独（nullable化のみで終える）
- `AuthSession.userId`を`String?`にするだけでは、根本原因（サーバーがユーザー情報を返さない設計になっている）は解消されず、呼び出し側（将来のViewModel実装）全てに「ログインしたのに自分が誰か分からない」ケースのUIハンドリングを強制することになる。ログインの基本機能が成立しないケースを正常系の一部として扱うのは過剰な防御であり、案Aで契約上`user`を必須にしたうえで、契約違反時のみ例外とする方が設計として筋が良いと判断した。

## 影響
- `docs/技術設計書.md` 6-1章: `POST /auth/login` のレスポンス定義を「`access_token`のみ」から「`User`、`access_token`」に修正（`POST /users`と同一形式に統一）
- `docs/技術設計書.md` 11-1章: DeveloperAgentへの修正指示（対象ファイル一覧）を追記
- 実装への影響（DeveloperAgent対応、詳細は技術設計書11-1章）
  - `AuthMapper.kt`: `user?.userId.orEmpty()`を廃止し、`user`が`null`の場合は例外を送出する実装へ変更
  - `AuthDto.kt`: `AuthSessionResponseDto`のKDocを更新（`user`は業務上必須であることの明記）
  - `ApiService.kt`: 型定義の変更は不要（既に`AuthSessionResponseDto`を共用しておりnullable設計だったため）。KDocのみ更新
  - `AuthRepositoryImpl.kt` / `AuthSession.kt` / `AuthRepository.kt`: 変更不要
  - `AuthMapperTest.kt`: 「ログインレスポンス相当(userなし)ではuserIdが空文字列にフォールバックする」テストケースを、例外がスローされる新しい期待動作に合わせて更新（TesterAgent連携）
- サーバーサイド（Cloud Functions等）の `/auth/login` 実装が `user` を返すようにする対応が別途必要。これは技術設計書10章#6（サーバーサイド技術選定）と同様、DeveloperAgentのサーバー実装スコープであり、本ADRはクライアント・サーバー間のインターフェース契約の変更のみを扱う

## 関連
- `docs/技術設計書.md` 6-1章
- `docs/adr/0003-auth-and-phone-verification.md`
- `docs/adr/0006-verify-otp-new-vs-existing-user.md` — `POST /auth/login`を`POST /auth/phone/verify`に統合し廃止した後継の判断
- `docs/test-plan.md` 4-1章（TesterAgentによる問題発見の経緯）
