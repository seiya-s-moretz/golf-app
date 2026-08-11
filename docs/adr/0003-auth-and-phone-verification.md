# ADR-0003: 認証基盤の新設とSMS OTPによる簡易本人確認方式の採用

## ステータス
承認・確定（2026-08-12、プロダクトオーナー確認によりSMS送信ベンダーをTwilioに確定。技術設計書10章#7参照）

## コンテキスト
既存技術設計（`D:\勉強\golf` 配下資料）には、ユーザー登録・ログイン・認証トークンに関する定義が一切存在しない。`ApiService.kt` にも認証ヘッダーの概念がなく、`GET /users/{id}` 等のAPIも認証なしで呼べる前提になっている。

PRD（`docs/要件定義書.md` 3-1章）は「SMS等による本人確認（アカウント登録時）」をMVP必須要件として追加した。「詳細な実装方式はArchitectAgentが判断」と明記されている。

本人確認を実装するには、最低限「誰が」その電話番号を確認したかをAPI呼び出し全体で識別できる認証の仕組みが必要であり、本人確認機能単体では実装できない。そのため、認証基盤そのものを本ADRの範囲で新設する。

## 決定
### 本人確認方式: 電話番号 + SMS OTP（ワンタイムパスワード）
- 理由: (1) PRDが「SMS等」と例示している、(2) 身分証明書アップロード等の強固な本人確認はPRD6章でPhase 2以降と明記されており、MVPでは「なりすまし・Bot登録の抑止」程度の軽量な確認で十分、(3) 電話番号は個人に紐付く再利用しづらい識別子であり、複数アカウント作成の抑止にも一定の効果がある、(4) 実装パターンが確立しており技術的リスクが低い

フロー: `POST /auth/phone/otp` でOTP送信 → `POST /auth/phone/verify` で検証・`registration_token` 発行 → `POST /users` でプロフィール登録とアカウント作成を同時に行い `access_token` を発行、という3段階とする（技術設計書6-1章）。

### 認証方式: Bearer トークン
- アカウント作成後に発行される `access_token` を `Authorization: Bearer <token>` ヘッダーで送信する、ステートレスなトークン認証を採用する。
- トークンの具体的な実装形式（JWT／不透明トークン+サーバー側セッションストア等）・有効期限・リフレッシュ方式は、サーバーサイド技術選定（TypeScript(Node.js) + Cloud Functions for Firebase、Firestore。技術設計書10章#6、確定）を踏まえDeveloperAgent着手時に決定する。本ADRでは「クライアントがBearerトークンを保持し全APIリクエストに付与する」というインターフェース契約のみを定める。
- 技術設計書10章#4・6-9章・ADR-0007のとおり、通報管理（簡易管理画面）向けの管理者認可もこの同じBearerトークン基盤を流用する（`User.is_admin`フラグによる追加検証のみ）。別建ての管理者認証基盤は設けない。

### SMS送信ベンダー: Twilio（**確定**、2026-08-12プロダクトオーナー確認）
- 本ADRで決定した「電話番号+OTP、自前API方式（`POST /auth/phone/otp`→`POST /auth/phone/verify`→`POST /users`）」というクライアント向けインターフェース契約は変更しない。
- サーバーサイド実装（Cloud Functions for Firebase）が `POST /auth/phone/otp` の処理内で、SMS送信の呼び出し先としてTwilioを利用する。国内SMS到達率・実装実績の観点から採用。AWS SNS等の他候補との詳細な費用比較は行っていないが、事業判断としてTwilio採用がプロダクトオーナーにより確定した。
- クライアント（Android）側の画面・API呼び出しへの影響はない（SMS送信はサーバー内部の実装詳細であり、クライアントは従来通り`POST /auth/phone/otp`を呼ぶのみ）。

## 代替案（検討したが不採用）
### 案: メールアドレス確認（メールOTPまたはマジックリンク）
実装コストは同等だが、PRDが「SMS等」と明記しており、また電話番号のほうが1人1端末に紐付きやすくなりすまし抑止効果が高いと判断し、SMSを第一候補とした（PRD文言上「等」を含むため他方式を完全排除するものではないが、本ADRではSMSを採用方式として決定する）。

### 案: 本人確認を設けず通報・ブロックのみで安全性を担保する
PRD3-1章・6章で「SMS等による簡易本人確認」がMust要件として明記されているため不採用。

### 案: Firebase Authentication（Phone Auth）への全面移行（2026-08-12検討・不採用）
サーバーサイドにCloud Functions for Firebaseを採用したことに伴い、SMS OTP送信・検証をFirebase Phone Authに全面移行する案も検討された。Firebase側でOTP送受信・検証ロジックを肩代わりできる利点はあるが、(1) 既に`POST /auth/phone/otp`→`POST /auth/phone/verify`→`POST /users`という3段階フローと対応するクライアント側認証3画面（電話番号入力・OTP認証・プロフィール初期登録）がADR-0006まで含めて設計・実装済みであり、作り直しのコストが移行メリットを上回る、(2) `POST /auth/phone/verify`に統合した新規/既存ユーザー判別ロジック（ADR-0006）はFirebase Phone Authの標準フローに存在せず、移行すると同等の作り直しが必要になる、という理由でプロダクトオーナーが明確に不採用と判断した。サーバー側のSMS送信部分のみTwilio呼び出しに置き換える方式（採用案）であれば、クライアント側の作り直しを発生させない。

## 影響
- 新規エンティティ `PhoneVerification`、`User` への `phone_number` / `phone_verified` / `phone_verified_at` / `is_admin` 追加（技術設計書5章）
- 新規API `POST /auth/phone/otp`, `POST /auth/phone/verify`, `POST /users`（技術設計書6-1章。`POST /auth/login`はADR-0006により廃止済み）
- 新規ディレクトリ `ui/screen/auth/`、`domain/repository/AuthRepository.kt` 等の追加（技術設計書4章）
- `NetworkModule` にBearerトークン付与のInterceptorを追加する必要がある
- サーバーサイド実装（Cloud Functions for Firebase）に、`POST /auth/phone/otp` からTwilio APIを呼び出すSMS送信処理を追加する必要がある（DeveloperAgentのサーバー実装スコープ）
- 通報管理（簡易管理画面）の管理者認可（`is_admin`検証）が本ADRのBearerトークン基盤に追加で乗る（技術設計書6-9章、ADR-0007）

## 関連
- `docs/技術設計書.md` 2章、5-2章（PhoneVerification）、6-1章（API）、6-9章、10章#6・#7
- `docs/adr/0005-login-response-user-id.md` — `POST /auth/login`のレスポンスに`User`を含める判断（本ADR策定時の6-1章の記述漏れの是正）
- `docs/adr/0006-verify-otp-new-vs-existing-user.md` — `POST /auth/phone/verify`に新規/既存ユーザー判別を統合し`POST /auth/login`を廃止した判断（本ADRで定めた3段階フローのうち「検証」と「ログイン」を1エンドポイントに統合。`POST /auth/phone/otp`→`POST /auth/phone/verify`→（新規のみ）`POST /users`という段階構成自体は維持）
- `docs/adr/0007-report-admin-panel.md` — 通報管理（簡易管理画面）の管理者認可に本ADRのBearerトークン基盤+`is_admin`フラグを流用する判断
