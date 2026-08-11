# ADR-0006: `POST /auth/phone/verify` に新規/既存ユーザー判別を統合し、`POST /auth/login` を廃止する

## ステータス
承認

## コンテキスト
DeveloperAgentによる認証フロー画面（電話番号入力〜OTP認証〜プロフィール初期登録）の実装中に、設計上の欠落が発見された（詳細は`app/src/main/java/com/golfmatch/app/ui/viewmodel/OtpVerificationViewModel.kt`のKDoc参照）。

技術設計書（旧版）6-1章は次の2エンドポイントを別々に定義していた。
- `POST /auth/phone/verify`: OTP検証。成功時に本登録用の一時トークン`registration_token`を返す
- `POST /auth/login`: 既存ユーザーの再ログイン（電話番号+OTPを再度検証）

しかし、**OTP入力画面に到達した時点で、そのユーザーが「新規（プロフィール未登録）」か「既存（登録済み）」かをクライアントがどう判別してどちらのAPIを呼ぶべきかを決めるかの設計が存在しなかった**。`verify`のレスポンス（`VerifyOtpResponseDto`）にも新規/既存を示すフィールドは無い。

DeveloperAgentは暫定実装として、「まず`POST /auth/login`を試行し、失敗したら未登録とみなして`POST /auth/phone/verify`を呼ぶ」という順序で分岐させていたが、この方式には以下の既知の課題があった。
1. ログイン失敗が「アカウント未登録」によるものか、「OTP不一致・期限切れ・サーバーエラー」等の別理由によるものかをクライアントがエラー内容から確定的に区別できず、後者の場合も誤って新規登録フローに倒れる恐れがある
2. 同一のOTPコードを2つの異なるエンドポイント（`verify`→`login`）に対して連続して送信する前提になっており、`PhoneVerification`（技術設計書5-2章）が「OTPは1回限り有効」という実装だった場合、2回目の呼び出しが失敗するという設計上の齟齬がありうる

## 検討した案
### 案1: `POST /auth/phone/verify`のレスポンスに`is_new_user`フラグを追加し、クライアントはこのフラグで確定的に分岐する
判別は確実になるが、`POST /auth/login`という別エンドポイントを維持したままだと、「新規/既存の判別はverifyで行うが、既存ユーザーの認証（アクセストークン発行）は別途loginを呼ぶ」という2段階の呼び出しが必要になり、同一OTPを2エンドポイントで消費する上記課題2が残る。

### 案2: `POST /auth/phone/verify`のレスポンスに、既存ユーザーであれば`user`情報自体を含め、`user`の有無で分岐する
ADR-0005（`POST /auth/login`のレスポンスに`User`を含める判断）との一貫性はあるが、「`user`フィールドの有無」という間接的な情報でクライアントが分岐判断をすることになり、`null`と「パース失敗等による欠落」を区別できない（ADR-0005で問題になった暗黙フォールバックと同種のリスクを、今度はクライアント側の分岐ロジックに持ち込むことになる）。

### 案3（採用）: `POST /auth/phone/verify`一本で認証を完結させ、`POST /auth/login`を廃止する。レスポンスには明示的な`is_new_user`フラグを設け、フラグの値に応じて既存ユーザーなら`user`+`access_token`（セッション開始まで完了）、新規ユーザーなら`registration_token`を返す
案1・案2の要素を組み合わせつつ、根本原因（同じOTP検証という1つの行為に対し、なぜ2つのAPIが存在するのか）を解消する。

## 決定
**案3を採用する。**

### 1. `POST /auth/phone/verify` にOTP検証と新規/既存ユーザーの判定・認証完了までを統合する
サーバー側処理を以下のように変更する。
1. `PhoneVerification`に対しOTPの正当性を検証する（既存のまま、変更なし）
2. 検証成功時、`phone_number`が一致し`phone_verified=true`の`User`が既に存在するかを判定する
3. 存在する場合（既存ユーザー）: そのユーザーのセッションを開始し`access_token`を発行する（旧`POST /auth/login`が行っていた処理をここに統合）。レスポンスは`is_new_user=false`、`user`、`access_token`
4. 存在しない場合（新規ユーザー）: 本登録（`POST /users`）に進むための一時トークン`registration_token`を発行する。レスポンスは`is_new_user=true`、`registration_token`

`is_new_user`はクライアントが分岐に用いる唯一の確定的な判定材料とする。他フィールドの有無（nullable）による暗黙の分岐は行わない（ADR-0005で得た教訓：契約上の必須/任意をnullableだけで表現すると、契約違反時のフォールバック先を誤りやすい）。

### 2. `POST /auth/login` を廃止する
電話番号入力→OTP送信→OTP検証、という一連の画面フロー自体が新規登録・再ログインの両方で完全に同一であり（実際、DeveloperAgentが実装したOTP認証画面は最初からこの両方を1画面で扱う設計になっていた）、クライアントが「これは登録用の検証なのかログイン用の検証なのか」を画面遷移前に知る必要はない。1回のOTP検証で判定と認証を完結させることで、以下が同時に解決する。
- 新規/既存ユーザーの判定がエラーハンドリングのヒューリスティックではなく、サーバーが返す確定的なフラグになる（コンテキストの課題1の解消）
- 同一OTPを2つのエンドポイントに渡す必要がなくなる（コンテキストの課題2の解消）
- クライアントの認証フロー実装が「OTP検証を1回呼び、結果に応じて2画面のどちらかに遷移する」というシンプルな分岐に単純化される

## 影響（技術設計書・ADRへの反映）
- `docs/技術設計書.md` 6-1章: `POST /auth/phone/verify`のレスポンス定義を変更（`is_new_user`追加、既存ユーザー分岐で`user`+`access_token`を返す）。`POST /auth/login`のエントリを削除し、廃止の経緯を明記
- `docs/adr/0005-login-response-user-id.md`: `POST /auth/login`というエンドポイント自体は本ADRにより廃止されるため、ADR-0005が定めた「レスポンス形式（`user`+`access_token`、暗黙フォールバック禁止）」という**原則**は`POST /auth/phone/verify`の既存ユーザー分岐に引き継がれる形で存続する。ADR-0005自体は無効化しない（原則は正しかった。適用対象のエンドポイントが統合されただけ）

## 実装への影響（DeveloperAgent対応）

| ファイル | 修正内容 |
|---|---|
| `app/src/main/java/com/golfmatch/app/data/dto/AuthDto.kt` | `VerifyOtpResponseDto`を拡張する。`is_new_user: Boolean`を追加し、既存の`AuthSessionResponseDto`をそのまま再利用する形で`session: AuthSessionResponseDto?`（`is_new_user=false`時のみ非null）を追加、`registration_token: String?`をnullable化（`is_new_user=true`時のみ非null）に変更する。`LoginRequestDto`は削除する |
| `app/src/main/java/com/golfmatch/app/data/api/ApiService.kt` | `@POST("auth/login")`の`login()`メソッドを削除する。`verifyPhoneOtp()`のKDocを更新し、レスポンスが新規/既存ユーザーで内容の異なる`VerifyOtpResponseDto`を返す旨を明記する |
| `app/src/main/java/com/golfmatch/app/domain/model/AuthSession.kt` | 同ファイルに新しいsealed interface `PhoneOtpVerificationResult`を追加する。`ExistingUser(val session: AuthSession) : PhoneOtpVerificationResult`と`NewUser(val registrationToken: RegistrationToken) : PhoneOtpVerificationResult`の2ケースとする |
| `app/src/main/java/com/golfmatch/app/domain/repository/AuthRepository.kt` | `login()`メソッドを削除する。`verifyPhoneOtp()`の戻り値型を`RegistrationToken`から`PhoneOtpVerificationResult`に変更する |
| `app/src/main/java/com/golfmatch/app/domain/usecase/VerifyPhoneOtpUseCase.kt` | 戻り値型を`PhoneOtpVerificationResult`に変更する |
| `app/src/main/java/com/golfmatch/app/domain/usecase/LoginUseCase.kt` | 削除する（役割は`VerifyPhoneOtpUseCase`に統合されたため） |
| `app/src/main/java/com/golfmatch/app/data/repository/impl/AuthRepositoryImpl.kt` | `login()`実装を削除する。`verifyPhoneOtp()`内で`VerifyOtpResponseDto.isNewUser`により分岐し、既存ユーザー分岐では`sessionManager.updateSession(...)`をこの中で呼び出す（`registerUser()`内の既存の呼び出しパターンを踏襲） |
| `app/src/main/java/com/golfmatch/app/data/mapper/AuthMapper.kt` | `VerifyOtpResponseDto.toDomain(): PhoneOtpVerificationResult`に変更する。`is_new_user`で分岐し、`false`側では既存の`AuthSessionResponseDto.toDomain()`ロジック（`user`必須・null時例外送出、ADR-0005）をそのまま再利用する |
| `app/src/main/java/com/golfmatch/app/ui/viewmodel/OtpVerificationViewModel.kt` | 現状の「まず`loginUseCase`を試行し、失敗したら`verifyPhoneOtpUseCase`を呼ぶ」というtry-catchベースの分岐ロジックを全廃し、`verifyPhoneOtpUseCase`の呼び出し1回のみに統合する。戻り値が`PhoneOtpVerificationResult.ExistingUser`なら`loginSuccess = true`としてホーム画面へ、`NewUser`なら`registrationToken`をセットしプロフィール初期登録画面へ遷移させる。`loginUseCase`への依存（コンストラクタ引数）を削除する。「## 要確認事項」として記載されていたKDocの記述は解消済みのため削除する |
| `app/src/main/java/com/golfmatch/app/ui/viewmodel/OtpVerificationUiState`（同ファイル内） | 構造自体の変更は不要（`loginSuccess`・`registrationToken`の2フィールドで両分岐を表現できる） |
| `app/src/test/java/com/golfmatch/app/data/mapper/AuthMapperTest.kt` 等 | `VerifyOtpResponseDto.toDomain()`の新しい分岐仕様（`is_new_user`による分岐、`ExistingUser`/`NewUser`）に合わせてテストケースを更新する（TesterAgentと連携） |

サーバーサイド（Cloud Functions等の`/auth/phone/verify`実装）が、OTP検証成功後に`User`の存在確認とセッション発行までを行うよう変更する対応、および`/auth/login`エンドポイント自体の削除も別途必要になるが、これは技術設計書10章#6（サーバーサイド技術選定）と同様にDeveloperAgentのサーバー実装スコープであり、本ADRはクライアント・サーバー間のインターフェース契約の変更のみを扱う。

## 関連
- `docs/技術設計書.md` 6-1章
- `docs/adr/0003-auth-and-phone-verification.md`（認証基盤・3段階フローの元設計。本ADRは「検証」と「ログイン」の2エンドポイントを1つに統合するものであり、`POST /auth/phone/otp`→`POST /auth/phone/verify`→（新規なら）`POST /users`という段階構成自体は維持する）
- `docs/adr/0005-login-response-user-id.md`（レスポンスに`user`を含める原則・暗黙フォールバック禁止の原則は本ADRに引き継がれる）
- `app/src/main/java/com/golfmatch/app/ui/viewmodel/OtpVerificationViewModel.kt`（問題発見の経緯となった暫定実装のKDoc）
