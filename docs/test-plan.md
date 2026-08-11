# テスト計画：ゴルフマッチングアプリ（雛形・共通基盤フェーズ）

作成日: 2026-08-11
更新日: 2026-08-12（ADR-0006対応検証を反映）
作成者: TesterAgent
対象: DeveloperAgent「プロジェクト雛形・共通基盤」フェーズ成果物、および認証フロー画面（電話番号入力〜OTP認証〜プロフィール初期登録）実装（ADR-0006対応、コミット7f1b9c7）
参照元: `docs/要件定義書.md`（PRD）, `docs/技術設計書.md`（技術設計）, `docs/adr/` 配下ADR, `README.md`

---

## 0. テスト対象範囲

現時点では画面UI（Compose Screen/Container）の大部分は未実装（次フェーズ）のため、UIテスト（Compose UIテスト・Android計測テスト）は引き続き対象外とする。
ただし2026-08-12更新分として、認証フロー（電話番号入力〜OTP認証〜プロフィール初期登録）の`ViewModel`層・`domain/usecase`・`data/mapper`はJVMユニットテストの対象に含める（詳細は2-5章）。

本フェーズで実装済みの以下をテスト対象とする。

- ビルドの健全性（`:app:assembleDebug`）
- `domain/model` のEntity定義と技術設計書5章のデータモデル定義との整合性
- `domain/usecase` のロジック（Repositoryへの委譲の正しさ、承認/却下等の分岐ロジック）
- `data/mapper` のDTO⇔Domainモデル変換の正確性
- 技術設計書・ADRとコードの整合性チェック（矛盾・不整合の洗い出し）

Repository実装（`data/repository/impl`）は現時点でApiServiceへの薄い委譲のみであり、実際のFirestore/Cloud
Functions接続・サーバー側バリデーション（レコメンドスコアリング、capacity超過チェック、Connectionアクセス制御等）は
未実装（次フェーズ）である。これらはRepository経由のI/Oが必要でありクライアント単体では検証不能なため、
本テスト計画では「テスト対象外」として明記する（1章参照）。

---

## 1. テスト対象外（未実装のため）

| 項目 | 理由 |
|---|---|
| 各画面のCompose UI（Screen/Container/ViewModel） | 未実装（次フェーズ、README「現在の状態」参照） |
| レコメンドスコアリングロジック本体（スコア差±10:40点／エリア一致:40点／目的一致:20点、60点以上で推薦） | `GetRecommendUsersUseCase`のコメントに明記のとおり「サーバー側で適用済み」が前提の設計であり、クライアントには実装がない（`UserRepositoryImpl.getRecommendedUsers`はAPI結果をそのまま返すのみ）。サーバーサイド（Cloud Functions）実装は次フェーズのため、スコアリングアルゴリズムそのものの単体テストは実施不可能 |
| ラウンド参加承認時の`capacity > current`再検証、マッチング申請の重複防止(`PENDING`1件まで) | サーバー側バリデーション。クライアント側Repository/UseCaseには実装なし |
| Connectionの有無・ブロック関係によるメッセージ送信可否のアクセス制御 | サーバー側の責務（ADR-0004）。クライアント側には実装なし |
| ブロックによるおすすめユーザー・掲示板からの除外 | サーバー側フィルタ（技術設計書6-5章・6-6章）。クライアント側には実装なし |
| Firebase実接続（Auth/Firestore/Functions） | `google-services.json`はプレースホルダー、Cloud FunctionsのベースURLもプレースホルダー（README・NetworkModule） |
| Hilt DIグラフの実行時解決検証 | Android計測テスト（instrumented test）の範囲であり、本フェーズはJVMユニットテストのみ実施 |

---

## 2. テスト観点・テストケース一覧

### 2-1. ビルド健全性
| # | 観点 | 実施内容 | 結果 |
|---|---|---|---|
| B-1 | `:app:assembleDebug` が成功する | Gradleビルドを実行 | OK |
| B-2 | `:app:testDebugUnitTest` が成功する | JVMユニットテスト一式を実行 | OK（62件成功、0失敗） |

### 2-2. domain/model（Entity定義照合）
技術設計書5章の各Entityと `domain/model` のプロパティ・型・null許容・enum値を1件ずつ照合（コードレビューにて実施、下記は照合結果のサマリ）。

| Entity | 照合結果 |
|---|---|
| User | 一致。`location`→`area_id`（ADR-0002）、`phone_*`3項目、`status`が反映済み |
| RoundEvent | 一致。`current`の意味論変更（ADR-0001、承認時のみ加算）はコメントで明記 |
| RoundJoinRequest | 一致。status enum(PENDING/APPROVED/REJECTED)、respondedAt nullable |
| BoardPost | 一致。変更なし |
| MatchRequest | 一致。status enum(PENDING/ACCEPTED/REJECTED) |
| Connection | 一致。sourceType enum(MATCH_REQUEST/ROUND_JOIN) |
| Message / Conversation | 一致。readAt nullable、Conversation集約モデルも6-7章のレスポンス項目と一致 |
| Report | 一致。reasonCategoryにDATING_SOLICITATIONを含む5値、status 4値 |
| Block | 一致 |
| Area(AreaMaster) | 一致 |
| PhoneVerification | 概ね一致。`otp_code_hash`はクライアント側で意図的に除外（コメントで理由明記、妥当な設計判断） |
| AuthSession / RegistrationToken | 技術設計書5章に明示のテーブル定義はないが（4章のディレクトリ構成上の言及のみ）、ADR-0003のインターフェース契約と矛盾しない |

ID型はUUID型指定に対し全て`String`実装（README記載の意図的な判断、Firestoreドキュメント運用を踏まえたもの）。日時型は`kotlinx.datetime.Instant`（README記載の意図的な判断）。いずれも設計書からの逸脱ではあるが、README上で理由が明記されており不整合とはみなさない。

テストコード: `app/src/test/java/com/golfmatch/app/domain/model/PurposeTest.kt`, `EntityDataModelTest.kt`

### 2-3. domain/usecase（ロジック検証）
| # | UseCase | 検証内容 |
|---|---|---|
| U-1 | GetRecommendUsersUseCase | Repository結果の素通しのみ確認（スコアリングロジック自体はテスト対象外、2-1参照） |
| U-2 | ApproveRoundJoinUseCase | `approve=true`/`false`で`approveJoinRequest`/`rejectJoinRequest`を正しく呼び分けるか（分岐ロジック） |
| U-3 | ApplyRoundJoinUseCase / CreateRoundEventUseCase | 引数のRepositoryへの委譲 |
| U-4 | SendMatchRequestUseCase / RespondMatchRequestUseCase | 委譲、および承認/却下の分岐ロジック |
| U-5 | GetConversationsUseCase / GetMessagesUseCase / SendMessageUseCase | 委譲。`GetMessagesUseCase`のデフォルト引数（`before=null`, `limit=50`）の確認 |
| U-6 | BlockUserUseCase / UnblockUserUseCase | 委譲 |
| U-7 | RequestPhoneOtpUseCase / VerifyPhoneOtpUseCase / RegisterUserUseCase | 委譲。`VerifyPhoneOtpUseCase`は`PhoneOtpVerificationResult`（`ExistingUser`/`NewUser`両ケース）をそのまま返すことを確認（ADR-0006、2-5章参照） |
| U-8 | GetAreasUseCase / PostBoardMessageUseCase / SubmitReportUseCase | 委譲 |

テストコード: `app/src/test/java/com/golfmatch/app/domain/usecase/*.kt`（Fakeリポジトリを用いた委譲検証、`testutil/FakeRepositories.kt`）

### 2-4. data/mapper（DTO⇔Domain変換）
| # | Mapper | 検証内容 |
|---|---|---|
| M-1 | UserMapper | 全フィールド変換、`area`ネスト展開からの`areaId`抽出、`purpose`の名前/日本語ラベル両対応、`phoneVerifiedAt`のnull/非null、`status`不正値での例外 |
| M-2 | RoundEventMapper | 全フィールド変換（`current`は単純な値渡し） |
| M-3 | RoundJoinRequestMapper | PENDING/APPROVED/REJECTEDの状態別変換、`respondedAt`のnull許容 |
| M-4 | BoardPostMapper | 全フィールド変換 |
| M-5 | MatchRequestMapper | PENDING/ACCEPTEDの状態別変換 |
| M-6 | MessageMapper | 未読(`readAt=null`)/既読の変換 |
| M-7 | ConversationMapper | ネストしたUser/Message変換、`lastMessage=null`（未メッセージ会話）の変換 |
| M-8 | ReportMapper | 5種の`reasonCategory`、`reasonText`のnull許容、`BOARD_POST`対象の変換 |
| M-9 | AreaMapper | `isActive=false`（将来エリアの先行登録想定）の変換 |
| M-10 | AuthMapper | `VerifyOtpResponseDto`→`PhoneOtpVerificationResult`（ADR-0006、`is_new_user`分岐、2-5章参照）、`AuthSessionResponseDto`→`AuthSession`（`user`必須・null時例外送出、ADR-0005） |

テストコード: `app/src/test/java/com/golfmatch/app/data/mapper/*.kt`

---

## 2-5. 認証フロー（電話番号入力〜OTP認証〜プロフィール初期登録、ADR-0006対応）

コミット7f1b9c7「fix: OTP検証に新規/既存ユーザー判定を統合しauth/loginを廃止(ADR-0006)」により、`POST /auth/login`が廃止され`POST /auth/phone/verify`に`is_new_user`フラグが統合された。これに伴うクライアント実装をADR-0006「実装への影響」表（55〜65行目）と1件ずつ照合した。

### 2-5-1. ADR-0006「実装への影響」表との照合結果

| # | 対象ファイル | 照合結果 |
|---|---|---|
| A-1 | `AuthDto.kt` | 一致。`VerifyOtpResponseDto`が`isNewUser: Boolean`, `session: AuthSessionResponseDto?`, `registrationToken: String?`（nullable化）を持つ。`LoginRequestDto`は削除済み |
| A-2 | `ApiService.kt` | 一致。`@POST("auth/login")`の`login()`メソッドは削除済み。`verifyPhoneOtp()`のKDocが新規/既存ユーザーで内容の異なるレスポンスを返す旨を明記 |
| A-3 | `AuthSession.kt` | 一致。`sealed interface PhoneOtpVerificationResult`が追加され、`ExistingUser(session: AuthSession)`/`NewUser(registrationToken: RegistrationToken)`の2ケース |
| A-4 | `AuthRepository.kt` | 一致。`login()`は削除済み。`verifyPhoneOtp()`の戻り値型が`PhoneOtpVerificationResult`に変更 |
| A-5 | `VerifyPhoneOtpUseCase.kt` | 一致。戻り値型が`PhoneOtpVerificationResult` |
| A-6 | `LoginUseCase.kt` | 一致。ファイル自体が削除済み（`app/src/main/java/com/golfmatch/app/domain/usecase/LoginUseCase.kt`は存在しない） |
| A-7 | `AuthRepositoryImpl.kt` | 一致。`login()`実装は削除済み。`verifyPhoneOtp()`内で`isNewUser`により分岐し、`ExistingUser`側でのみ`sessionManager.updateSession(...)`を呼ぶ |
| A-8 | `AuthMapper.kt` | 一致。`VerifyOtpResponseDto.toDomain(): PhoneOtpVerificationResult`が`isNewUser`で確定的に分岐。`false`側では既存の`AuthSessionResponseDto.toDomain()`（`user`必須・null時`checkNotNull`で例外送出、ADR-0005の原則を引き継ぎ）をそのまま再利用している |
| A-9 | `OtpVerificationViewModel.kt` | 一致。`loginUseCase`への依存（コンストラクタ引数）は完全に削除され、`verifyPhoneOtpUseCase`の呼び出し1回のみに統合。try-catchベースの暫定分岐（旧KDocの「## 要確認事項」）は解消され記述も削除済み。`when(result)`で`ExistingUser`→`loginSuccess=true`、`NewUser`→`registrationToken`セットの分岐が実装されている |
| A-10 | `OtpVerificationUiState` | 一致。構造変更なし（`loginSuccess`・`registrationToken`の2フィールドで両分岐を表現） |

ADR-0006「実装への影響」表とコード変更の間に不整合は見つからなかった。`docs/技術設計書.md`6-1章（`POST /auth/phone/verify`のレスポンス定義、`POST /auth/login`廃止の記載）ともADR-0006と整合している。

### 2-5-2. テストケース

| # | 対象 | 検証内容 | 結果 |
|---|---|---|---|
| AUTH-1 | AuthMapper | `is_new_user=true`かつ`registration_token`ありで`NewUser`に変換される | OK |
| AUTH-2 | AuthMapper | `is_new_user=true`かつ`registration_token=null`（契約違反）で例外送出 | OK |
| AUTH-3 | AuthMapper | `is_new_user=false`かつ`session`ありで`ExistingUser`に変換される（内部で`user`必須の`AuthSessionResponseDto.toDomain()`を再利用） | OK |
| AUTH-4 | AuthMapper | `is_new_user=false`かつ`session=null`（契約違反）で例外送出 | OK |
| AUTH-5 | AuthMapper | `POST /users`相当（`user`あり）のレスポンス変換 | OK |
| AUTH-6 | AuthMapper | `AuthSessionResponseDto`の`user=null`（契約違反、ADR-0005の原則を`verify`既存ユーザー分岐にも適用）で例外送出 | OK |
| AUTH-7 | VerifyPhoneOtpUseCase | 引数(`phoneNumber`, `otpCode`)をそのまま委譲し`NewUser`を返す | OK |
| AUTH-8 | VerifyPhoneOtpUseCase | 引数をそのまま委譲し`ExistingUser`を返す（既存テストが`NewUser`ケースのみだったため、本更新でTesterAgentが追加） | OK |
| AUTH-9 | RequestPhoneOtpUseCase / RegisterUserUseCase | 委譲確認（既存踏襲） | OK |

テストコード: `app/src/test/java/com/golfmatch/app/data/mapper/AuthMapperTest.kt`, `app/src/test/java/com/golfmatch/app/domain/usecase/AuthUseCasesTest.kt`, `app/src/test/java/com/golfmatch/app/testutil/FakeRepositories.kt`（`FakeAuthRepository`が`PhoneOtpVerificationResult`を差し替え可能に更新されている）

### 2-5-3. カバレッジ上の観察（バグではない、参考）

- `OtpVerificationViewModel`（および電話番号入力・プロフィール初期登録の各ViewModel）に対するユニットテストは本更新時点で存在しない（`app/src/test/java/com/golfmatch/app/ui/viewmodel/`配下に該当ファイルなし）。ADR-0006の分岐（`ExistingUser`→`loginSuccess=true`、`NewUser`→`registrationToken`セット）はコードレビューでは`AuthMapperTest`/`AuthUseCasesTest`の分岐と整合していることを確認したが、ViewModelの`StateFlow`遷移自体を検証するテストは未整備。UI層のテスト方針（0章）が定まった段階でDeveloperAgent/TesterAgent間で追加を検討されたい（バグではないため差し戻し必須ではない）
- `AuthRepositoryImpl.verifyPhoneOtp()`は`ExistingUser`時のみ`sessionManager.updateSession(...)`を呼ぶという分岐ロジックを含むが、`data/repository/impl`配下は本テスト計画0章で「ApiServiceへの薄い委譲のみ」を理由にテスト対象外としてきた経緯があり、本更新でも同方針を踏襲し対象外のままとした。ただし本メソッドは単純な委譲を超えた条件分岐を含み始めているため、次フェーズで`ApiService`のFake/Mock整備とあわせてテスト対象化を検討する余地がある（参考情報として記録、差し戻し事項ではない）

---

## 3. テスト実行結果サマリ

### 3-1. 2026-08-11実施分（雛形・共通基盤フェーズ）
- 実行コマンド: `./gradlew :app:assembleDebug` / `./gradlew :app:testDebugUnitTest`
- ビルド: 成功
- ユニットテスト: 62件実行、成功62件、失敗0件、エラー0件
- テストファイル数: 18ファイル（domain/model 2、data/mapper 9、domain/usecase 6、testutil（Fixture/Fake）2）

### 3-2. 2026-08-12実施分（ADR-0006対応、コミット7f1b9c7検証）
- 実行コマンド: `./gradlew :app:assembleDebug` / `./gradlew :app:testDebugUnitTest`
- ビルド: 成功（`BUILD SUCCESSFUL`、警告なし）
- ユニットテスト: 66件実行、成功66件、失敗0件、エラー0件
  - 内訳の変化: `AuthMapperTest`が4件→6件（ADR-0006の`is_new_user`分岐4ケース追加、旧ログインレスポンス相当のテスト名を整理）、`AuthUseCasesTest`が3件のまま（`VerifyPhoneOtpUseCase`の`ExistingUser`分岐テストをTesterAgentが1件追加した一方、内容が更新されたテストと統合された結果件数据え置き）
  - 上記件数はTesterAgentが2-5-2章AUTH-8のテストケースを追加した後の数値（追加前は65件、追加分含め全件成功）
- リグレッション: 認証以外の既存テスト（domain/model, domain/usecase の他UseCase, data/mapper の他Mapper）はいずれも変更なく全件成功しており、リグレッションは確認されなかった

---

## 4. 発見した不整合・バグ報告（DeveloperAgentへの差し戻し事項）

### 4-1. 【解消済み】ログイン成功時にAuthSession.userIdが空文字列になる

**2026-08-12更新: 本件はADR-0005（暗黙フォールバック廃止・例外送出への変更）およびADR-0006（`POST /auth/login`廃止・`POST /auth/phone/verify`への統合）により解消されたことをコードで確認した。**

- 確認内容:
  - `POST /auth/login`エンドポイント自体が廃止され（`ApiService.kt`に`login()`メソッドは存在しない）、本項が指摘していた「ログインレスポンスに`user`が含まれない」という設計上の非対称性の発生源そのものが無くなった
  - 後継の`POST /auth/phone/verify`（既存ユーザー分岐）は、`AuthSessionResponseDto`（`user`必須）を再利用しており、`AuthMapper.kt`の`AuthSessionResponseDto.toDomain()`は`user`が`null`の場合に`checkNotNull`で例外を送出する実装になっている（空文字列への暗黙フォールバックは行われない）
  - `AuthMapperTest.kt`の`userを含まないレスポンスをtoDomainすると例外がスローされる(ADR-0005)`テストで、`IllegalStateException`が送出されることを確認済み（テスト実行結果: OK）
  - `VerifyOtpResponseDto.toDomain()`（`is_new_user`による分岐）についても、`is_new_user=false`かつ`session=null`という契約違反のケースで例外が送出されることを`AuthMapperTest.kt`で確認済み（2-5-2章AUTH-4）
- 結論: 「ログインできたのに自分が誰か分からない」状態（`userId`が空文字列のままサイレントに後続処理へ伝播する）は、コード上再現しないことを確認した。案A（レスポンスに`user`を含める）・案C一部（暗黙フォールバック廃止）の組み合わせという当初のADR-0005の決定が、ADR-0006によるエンドポイント統合後も一貫して維持されている。

<details>
<summary>以下、2026-08-11時点の原報告（記録として残す）</summary>

- 対象コード: `app/src/main/java/com/golfmatch/app/data/mapper/AuthMapper.kt`
  ```kotlin
  fun AuthSessionResponseDto.toDomain(): AuthSession = AuthSession(
      accessToken = accessToken,
      userId = user?.userId.orEmpty()
  )
  ```
- 技術設計書6-1章: `POST /auth/login`のレスポンス定義は「`access_token`」のみであり、`User`オブジェクトを含まない（`POST /users`（新規登録）のレスポンスは「作成された`User`、`access_token`」だが、ログインは異なる）。
- 一方 `domain.AuthSession.userId` は非null Stringであり、`AuthMapper`は`user`が存在しない場合に`userId`を空文字列`""`にフォールバックする実装になっている。
- 再現手順:
  1. `AuthSessionResponseDto(user = null, accessToken = "xxx")`を`toDomain()`する（設計書どおりのログインレスポンス相当）
  2. `AuthSession.userId`が`""`になる
- 期待される結果: ログイン後もアプリが「誰としてログインしたか」を認識できること（例えば`GET /users/{id}`呼び出し等、以降のユーザー操作に`userId`が必要な場面がある）
- 実際の結果: `userId`が空文字列となり、エラーにもならず後続処理へサイレントに伝播しうる
- 影響: ログインフロー実装時（次フェーズのViewModel実装）で、この空文字列を発見せず利用すると、誤ったユーザーIDでAPIを呼び出す、または画面遷移後にユーザー情報が正しく表示されない等の不具合につながる可能性がある
- 差し戻し内容（提案、決定はArchitectAgent/DeveloperAgentに委ねる）:
  - 案A: `POST /auth/login`のレスポンスにも`user`（または`user_id`）を含めるよう技術設計書6-1章を見直す（ArchitectAgent確認要）
  - 案B: レスポンスに`user`が含まれない場合は、`AuthRepositoryImpl.login()`内で`AuthSessionResponseDto`受領後に`GET /users/me`相当のAPIを追加で呼び出し`userId`を解決する
  - 案C: 空文字列への暗黙フォールバックをやめ、`user`が必須でない設計であれば`AuthSession.userId`自体をnullable化するか、取得できなかった場合に明示的な例外を投げる
  - このテストケースは当時 `app/src/test/java/com/golfmatch/app/data/mapper/AuthMapperTest.kt` の
    `ログインレスポンス相当(userなし)ではuserIdが空文字列にフォールバックする` に再現コードとして残していた（Pass/Failで判定するものではなく、現状挙動を明文化する目的）。
  - **2026-08-12注記**: 上記テストケースはADR-0005・ADR-0006対応により、期待動作が「例外送出」に変わったことに伴い`userを含まないレスポンスをtoDomainすると例外がスローされる(ADR-0005)`へ更新済み（DeveloperAgent側の対応、コミット履歴上はADR-0005対応時点および本ADR-0006対応時点の2回にわたり更新されている）。

</details>

### 4-2. 【軽微・参考】BlockDtoが未使用

- 対象コード: `app/src/main/java/com/golfmatch/app/data/dto/BlockDto.kt`
- 技術設計書6-3章のブロック関連API（`POST /users/{id}/block`, `DELETE /users/{id}/block`, `GET /users/me/blocks`）は、`ApiService`上では`GET /users/me/blocks`が`List<UserDto>`を返す実装になっており（`BlockedUsersUiState.blockedUsers: List<User>`と整合させるための妥当な選択）、`BlockDto`はどこからも参照されていない（対応する`BlockMapper`も存在しない）。
- 動作上の不具合ではなくデッドコードの指摘のみ。ブロック関連の管理API（例:ブロック日時を含む一覧等）を将来追加する場合の設計メモとして残すか、不要であれば削除を検討されたい。バグではないため差し戻し必須ではない。

### 4-3. 【軽微・参考】認証フロー画面のViewModelテスト・Repository実装層テストが未整備（2026-08-12追加、バグではない）

- ADR-0006対応の検証にあたり不整合・バグは見つからなかったが、2-5-3章で記載のとおり以下2点はテストカバレッジ上の観察事項として記録する。差し戻し必須ではないが、次フェーズでの検討を推奨する。
  1. `OtpVerificationViewModel`をはじめとする認証フロー画面のViewModelに対するユニットテストが未整備（`app/src/test/java/com/golfmatch/app/ui/viewmodel/`配下にテストファイルが存在しない）。UIテスト方針そのものが未確定（0章）であることが背景にあるため、UIテスト方針確定時にArchitectAgent/DeveloperAgent/TesterAgentで対象化を協議されたい
  2. `AuthRepositoryImpl.verifyPhoneOtp()`が`is_new_user`による条件分岐（`ExistingUser`時のみ`sessionManager.updateSession`を呼ぶ）を含むようになり、他Repository実装同様の「ApiServiceへの薄い委譲のみ」という前提（0章）から一部逸脱し始めている。`ApiService`のFake/Mock整備とあわせて、`data/repository/impl`配下のテスト対象化を次フェーズで検討する余地がある

---

## 5. 結論

現時点でテスト可能な範囲（domain/model, domain/usecase, data/mapper）について、技術設計書5章のデータモデル定義との齟齬は見つからなかった。UseCase層はいずれもRepositoryへの薄い委譲であり、レコメンドスコアリング等の主要ビジネスロジックはサーバー側実装待ちのためテスト対象外とした。

**2026-08-12追記（ADR-0006対応検証）**: コミット7f1b9c7（OTP検証への新規/既存ユーザー判定統合・`POST /auth/login`廃止）について、`./gradlew :app:assembleDebug`・`./gradlew :app:testDebugUnitTest`はいずれも成功（66件成功、0失敗）した。ADR-0006「実装への影響」表（55〜65行目）とコード変更（`AuthDto.kt`, `ApiService.kt`, `AuthSession.kt`, `AuthRepository.kt`, `VerifyPhoneOtpUseCase.kt`, `LoginUseCase.kt`削除, `AuthRepositoryImpl.kt`, `AuthMapper.kt`, `OtpVerificationViewModel.kt`）を1件ずつ照合し、不一致は見つからなかった。`docs/技術設計書.md`6-1章・11-2章もADR-0006と整合している。4-1章で報告していた「ログイン成功時にAuthSession.userIdが空文字列になる」バグは、`POST /auth/login`廃止と暗黙フォールバック廃止（ADR-0005の原則の引き継ぎ）により解消されたことをテストで確認し「解消済み」に更新した。新規のクリティカルなバグは発見しなかったが、`VerifyPhoneOtpUseCase`の`ExistingUser`分岐テストが欠落していたため追加した（2-5-2章AUTH-8）。また、認証フロー画面のViewModelテスト・Repository実装層テストの未整備を軽微な参考事項として記録した（4-3章）。
