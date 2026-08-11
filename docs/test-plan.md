# テスト計画：ゴルフマッチングアプリ（雛形・共通基盤フェーズ）

作成日: 2026-08-11
更新日: 2026-08-12（未実装だった残り全画面の実装・フッター5タブ化検証を反映、コミット590ebd9）
作成者: TesterAgent
対象: DeveloperAgent「プロジェクト雛形・共通基盤」フェーズ成果物、認証フロー画面実装（ADR-0006対応、コミット7f1b9c7）、および未実装だった残り全画面（ラウンド新規作成/詳細/参加申請一覧、受信マッチング申請一覧、通報画面・ブロック済みユーザー一覧、メッセージ一覧/スレッド、通報管理簡易管理画面）の実装・フッター5タブ化（ADR-0001・ADR-0007対応、コミット590ebd9）
参照元: `docs/要件定義書.md`（PRD）, `docs/技術設計書.md`（技術設計）, `docs/adr/` 配下ADR, `README.md`

---

## 0. テスト対象範囲

Compose Screen/Containerそのもの（描画・レイアウト・ナビゲーション遷移）は引き続きテスト対象外とする（Compose UIテスト・Android計測テストは実施しない、README「現在の状態」参照）。
ただし2026-08-12更新分として、認証フロー（電話番号入力〜OTP認証〜プロフィール初期登録）の`ViewModel`層・`domain/usecase`・`data/mapper`はJVMユニットテストの対象に含める（詳細は2-5章）。
さらに2026-08-12追加更新分（コミット590ebd9検証）として、単純な委譲を超える分岐ロジック（承認/却下、主催者判定、多重操作防止ガード等）を持つ`ViewModel`もJVMユニットテストの対象に含めることとした（Compose Screen自体は引き続き対象外。詳細は2-6-2章）。

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
| 各画面のCompose UI本体（Screen/Container、描画・レイアウト・ナビゲーション遷移） | Compose UIテスト・Android計測テストは方針上対象外（0章）。`ViewModel`層は分岐ロジックの有無に応じて2-5章・2-6章のとおり順次テスト対象に追加中 |
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

## 2-6. 未実装だった残り全画面の実装検証（コミット590ebd9、ADR-0001・ADR-0007対応）

コミット590ebd9「未実装だった残り全画面を実装しフッターを5タブ化」により、`NavGraph.kt`の`PlaceholderScreen`が全廃され、ラウンド新規作成/詳細/参加申請一覧、受信マッチング申請一覧、通報画面・ブロック済みユーザー一覧、メッセージ一覧/スレッド、通報管理（簡易管理画面、ADR-0007）が実装された（66ファイル、+約4046行）。あわせて`ReportStatus`のenumリネーム（`REVIEWED`→`REVIEWING`、`ACTION_TAKEN`→`RESOLVED`）、`Report.handledByUserId`/`handledAt`/`handlingMemo`追加、`User.isAdmin`追加が行われた。

### 2-6-1. 既存テストの追随修正が検証を弱めていないかの確認

DeveloperAgentは`ReportStatus`リネームに伴い`ReportMapperTest.kt`・`EntityDataModelTest.kt`・`TestFixtures.kt`・`FakeRepositories.kt`を「コンパイルを通すための最小限の追随修正」のみ行ったと報告していた。差分（`git show 590ebd9 -- <各ファイル>`）をレビューした結果、以下の**検証漏れ（弱化ではなく元々未検証だった箇所）**を確認した。

| # | ファイル | 確認内容 | 判定 |
|---|---|---|---|
| W-1 | `EntityDataModelTest.kt` | `ReportStatus`の4値セット確認テストの期待値が`{PENDING, REVIEWING, RESOLVED, DISMISSED}`に正しく更新されていた（アサーション削除・弱化なし） | 問題なし |
| W-2 | `ReportMapperTest.kt` | 既存3テストは`status`引数を`"PENDING"`/`"REVIEWING"`のみ使用しており、リネーム後も**`RESOLVED`・`DISMISSED`の2値が一度も変換テストされていなかった**（アサーション削除ではないが、4値中2値の検証が実質的に欠落） | **要補強→対応済み** |
| W-3 | `ReportMapperTest.kt` | `handledByUserId`・`handlingMemo`は常に`null`のケースしかテストされておらず、非null値（管理者が対応済みにした場合の実データ）での変換確認が一度も行われていなかった | **要補強→対応済み** |
| W-4 | `FakeRepositories.kt` / `ReportMapperTest.kt` | 新規追加された`ReportAdminSummaryDto.toDomain()`・`ReportAdminDetailDto.toDomain()`（管理者向け一覧・詳細変換）は、Fakeリポジトリ側の型（`ReportSummary`/`ReportDetail`）は追随済みだが、**変換ロジック自体のテストが1件も追加されていなかった**（新規追加コードのテストカバレッジ0%） | **要補強→対応済み** |
| W-5 | `UserMapper.kt`（`isAdmin`追加）/ `UserMapperTest.kt` | `UserMapperTest.kt`は本コミットで変更されていない。`isAdmin`フィールドのマッピングテストが1件も存在しなかった | **要補強→対応済み** |

**結論**: 既存テストの「アサーションが削られる」形の弱化（改悪）は見つからなかった。一方で、リネーム後の4値のうち2値（`RESOLVED`/`DISMISSED`）が未検証のまま残っていた点、および新規追加された管理者向けDTOマッパー・`isAdmin`が無テストだった点は実質的なカバレッジ欠落であり、本更新でTesterAgentが補強した（2-6-2章参照）。

### 2-6-2. 追加したテスト

#### data/mapper（`ReportMapperTest.kt`拡充、`UserMapperTest.kt`拡充）
- `ReportDto.toDomain()`: `ReportStatus`4値（PENDING/REVIEWING/RESOLVED/DISMISSED）すべての変換、`handledByUserId`/`handledAt`/`handlingMemo`のnull（未対応）・非null（対応済み）両方のケース
- `ReportAdminSummaryDto.toDomain()`（新規、`GET /admin/reports`一覧要素）: `reporter`・`targetSummary`を含む全項目変換、`REVIEWING`+handled系非nullのケース
- `ReportAdminDetailDto.toDomain()`（新規、`GET /admin/reports/{id}`）: `target_type=USER`時は`targetUser`のみ非null・`target_type=BOARD_POST`時は`targetBoardPost`のみ非null（片方のみ非nullという契約）をそれぞれ確認
- `UserDto.toDomain()`: `is_admin=true`/`false`両方のケース（ADR-0007の権限判定の起点）

#### domain/usecase（新規7 UseCase、`testutil/FakeRepositories.kt`を拡張して使用）
- `GetRoundEventUseCase`・`GetRoundJoinRequestsUseCase`（`RoundUseCasesTest.kt`に追加）
- `GetMatchRequestsUseCase`（`MatchUseCasesTest.kt`に追加、`direction=RECEIVED`/`SENT`両方の委譲確認）
- `GetBlockedUsersUseCase`（`UserUseCasesTest.kt`に追加）
- `GetAdminReportsUseCase`・`GetAdminReportDetailUseCase`・`UpdateReportStatusUseCase`（新規`ReportAdminUseCasesTest.kt`、`statusFilter=null`（全件取得）・`handlingMemo=null`の両ケースを含む）
- いずれも既存パターン同様、Repositoryへの薄い委譲であることのみを確認（`is_admin=true`検証等の認可ロジックはサーバー側の責務でありテスト対象外）

#### ui/viewmodel（新規、本更新でテストパッケージ自体を新設）
0章で従来「Compose UI/ViewModelはテスト対象外」としてきたが、4-3章（旧記録）で参考事項として挙げていたとおり、今回のコミットで単純な委譲でない分岐ロジックを持つViewModelが複数追加されたため、**ViewModel層（Compose非依存の`StateFlow`/ロジック部分）に限定してJVMユニットテストの対象に追加した**（Compose Screen自体のテストは引き続き対象外のまま）。`kotlinx-coroutines-test`を`gradle/libs.versions.toml`・`app/build.gradle.kts`に追加し、`testutil/MainDispatcherRule.kt`（`Dispatchers.Main`を`UnconfinedTestDispatcher`に差し替えるJUnit Rule）を新設した。

- `RoundDetailViewModelTest.kt`: `isOrganizer`判定（現在ユーザーIDと`RoundEvent.createdBy`の一致/不一致/未ログイン(null)の3パターン）、`load()`失敗時の`errorMessage`反映、`applyJoin()`の多重送信防止ガード（処理中に連打しても`ApplyRoundJoinUseCase`が1回しか呼ばれないことを`CompletableDeferred`で制御して確認）
- `RoundJoinRequestListViewModelTest.kt`: `respond(approve=true/false)`が承認/却下のどちらのAPIを呼ぶか、対象申請のみがリスト内で更新されること、`processingRequestId`による多重操作防止ガード
- `MatchRequestListViewModelTest.kt`: 同様（`direction=RECEIVED`固定での取得確認を含む）
- `ReportAdminDetailViewModelTest.kt`: `load()`時に`selectedStatus`/`handlingMemo`が通報詳細の値で初期化されること（`handlingMemo`未設定時は空文字）、`save()`が編集後の`selectedStatus`/`handlingMemo`（空白のみの場合はnull）で`UpdateReportStatusUseCase`を呼ぶこと。**加えて、後述4-4章のバグを再現・明文化するテストを1件追加した**

「残したテスト」（優先度の都合上、今回は実装を見送った範囲）: `MessageThreadViewModel`・`MessageListViewModel`・`BlockedUsersViewModel`・`CreateRoundViewModel`・`ReportViewModel`・`ReportAdminListViewModel`・`MyPageViewModel`（`isAdmin`追加分）のユニットテスト。理由は2-6-4章参照。

### 2-6-3. 設計整合性レビュー

`docs/技術設計書.md`3章・4章（ディレクトリ構成）・5章（データモデル）・6章（API）・7章（UiState）、`docs/adr/0001-round-join-approval-flow.md`、`docs/adr/0007-report-admin-panel.md`と実装を照合した。DeveloperAgentからのエスカレーション3件への判断は以下のとおり。

| # | エスカレーション事項 | 判断 |
|---|---|---|
| E-1 | `ui/screen/round/`パッケージが技術設計書4章の記載（`ui/screen/home/`配下）と異なる | **許容範囲の逸脱（バグではない）**。技術設計書4章はラウンド関連画面を`ui/screen/home/`直下に置く構成を示しているが、実装は独立した`ui/screen/round/`パッケージに分離している。機能・API・データモデルには一切影響せず、むしろラウンド関連3画面（`CreateRoundScreen`/`RoundDetailScreen`/`RoundJoinRequestListScreen`）をまとめる方が既存の`recommend/`・`message/`・`admin/`等の他パッケージ構成（機能単位でパッケージを切る方針）と一貫性が高い。技術設計書4章側の記載を実装に合わせて更新することを推奨する（ArchitectAgent確認事項、コード修正は不要）。あわせて、通報画面のファイル名も設計書4章`ReportDialog.kt`に対し実装は`ReportScreen.kt`、管理者向けUseCase名も設計書4章`GetReportsForAdminUseCase`/`GetReportDetailForAdminUseCase`に対し実装は`GetAdminReportsUseCase`/`GetAdminReportDetailUseCase`と、命名レベルの軽微な差異が複数あることも確認した（機能的な問題はない） |
| E-2 | 管理者向け一覧API`GET /admin/reports`のページネーション（`before`/`limit`）が未実装で`status`フィルタのみ | **技術設計書との不整合（要差し戻し）**。技術設計書6-9章は`GET /admin/reports`のクエリパラメータとして「`status?`」に加え「ページネーション（`before`, `limit`。既存一覧APIと同様の方式）」を明記しているが、実装（`ApiService.getAdminReports(status: String?)`、`ReportRepository.getAdminReports(statusFilter: ReportStatus?)`、`GetAdminReportsUseCase`）には`before`/`limit`に相当する引数が存在しない。ADR-0007は「想定利用者は少人数の運営メンバーのみ」「機能はMVPでは一覧確認とステータス変更に絞る」と述べているが、ページネーション自体の省略までは明言していない。運営が長期間放置すると一覧が際限なく肥大化しうる（既存の`GetMessagesUseCase`等では`before`/`limit`が実装されている）ため、技術設計書どおりに実装するか、または「MVPでは省略する」という設計判断をADR-0007に追記するかの決定をArchitectAgent/DeveloperAgentに委ねたい（4-5章に差し戻し事項として記録） |
| E-3 | 管理者向けDTO（`ReportAdminSummaryDto`等）は技術設計書6-9章に厳密なスキーマ定義がなく新規設計されたもの | **問題なし**。技術設計書6-9章はプローズ（文章）でレスポンス項目を規定しており（`reporter`・`target_summary`・`target_detail`等）、Kotlinの型定義そのものは示されていない。DeveloperAgentが新設した`ReportAdminSummaryDto`/`ReportAdminDetailDto`/`ReportAdminTargetUserDto`/`ReportAdminTargetBoardPostDto`は、6-9章の文章記述（USER/BOARD_POSTで片方のみ非nullの`target_detail`、`phone_number`等の機微情報を含めない`User`情報のみ、等）と1件ずつ突き合わせた結果、矛盾なく整合していることを確認した |

### 2-6-4. 今回テストを見送った範囲（優先度の都合、参考記録）

- `MessageThreadViewModel`・`MessageListViewModel`：分岐ロジックが薄い（単純な取得・送信の委譲＋`inputText`のトリム程度）ためRoundDetail等より優先度を下げた
- `BlockedUsersViewModel`・`ReportViewModel`・`ReportAdminListViewModel`・`CreateRoundViewModel`：`BlockedUsersViewModel`は`RoundJoinRequestListViewModel`と同型の`processingUserId`ガードを持つが実装パターンが酷似しており、`ReportViewModel`/`CreateRoundViewModel`はバリデーション分岐はあるが入力検証のみで外部I/O分岐は薄い。`ReportAdminListViewModel`はフィルタ変更で`load()`を呼び直すのみ
- `MyPageViewModel`の`isAdmin`追加分：既存フィールドと同様の単純代入（`isAdmin = user.isAdmin`）でありリスクが低い

いずれも次フェーズでのテスト追加候補として記録する（バグではない）。

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

### 3-3. 2026-08-12実施分（未実装だった残り全画面の実装検証、コミット590ebd9）
- 実行コマンド: `./gradlew :app:assembleDebug` / `./gradlew :app:testDebugUnitTest`
- ビルド: 成功（`BUILD SUCCESSFUL`）
- ユニットテスト: **104件実行、成功104件、失敗0件、エラー0件**（更新前66件 → 更新後104件、TesterAgentが38件追加）
  - 内訳: 既存`ReportMapperTest`3件→10件（2-6-2章）、既存`UserMapperTest`5件→7件、既存`RoundUseCasesTest`3件→5件、既存`MatchUseCasesTest`3件→5件、既存`UserUseCasesTest`3件→4件、新規`ReportAdminUseCasesTest`5件、新規`ui/viewmodel`パッケージ4ファイル計19件（`RoundDetailViewModelTest`5件、`RoundJoinRequestListViewModelTest`4件、`MatchRequestListViewModelTest`4件、`ReportAdminDetailViewModelTest`5件）
- 依存関係変更: `gradle/libs.versions.toml`・`app/build.gradle.kts`に`kotlinx-coroutines-test`を`testImplementation`として追加（ViewModelの`StateFlow`/`viewModelScope`検証に必要、本文書冒頭の制約に基づきTesterAgentが追加）
- リグレッション: 上記以外の既存テスト（domain/model、他のdomain/usecase、他のdata/mapper、2-5章の認証フロー関連）はいずれも変更なく全件成功しており、リグレッションは確認されなかった

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

### 4-4. 【バグ・要修正】ReportAdminDetailViewModel.save()に多重操作防止ガードが無い（2026-08-12追加、コミット590ebd9検証）

- 対象コード: `app/src/main/java/com/golfmatch/app/ui/viewmodel/ReportAdminDetailViewModel.kt`
- 同時期に追加された他画面のガード実装例（一貫性の比較対象）:
  - `RoundJoinRequestListViewModel.respond()`: `if (_uiState.value.processingRequestId != null) return`
  - `MatchRequestListViewModel.respond()`: 同上
  - `BlockedUsersViewModel.unblock()`: `if (_uiState.value.processingUserId != null) return`
- 一方`ReportAdminDetailViewModel.save()`は`isUpdating`という状態フィールド自体は持つが、`save()`冒頭でこれを参照した早期returnが実装されていない。
  ```kotlin
  fun save() {
      val state = _uiState.value
      viewModelScope.launch {
          _uiState.value = state.copy(isUpdating = true, errorMessage = null, updateSuccess = false)
          runCatching {
              updateReportStatusUseCase(reportId, state.selectedStatus, state.handlingMemo.ifBlank { null })
          }.onSuccess { ... }.onFailure { ... }
      }
  }
  ```
- 再現手順（`ReportAdminDetailViewModelTest.kt`の`【バグ】save処理中に再度saveを呼ぶとUseCaseが2回呼ばれてしまう(多重操作防止ガード欠落)`に再現テストとして追加済み、Pass=バグ挙動が現状のまま存在することを確認）:
  1. `save()`を呼ぶ（`updateReportStatusUseCase`が呼ばれ、レスポンス待ちの間`isUpdating=true`になる）
  2. `updateReportStatusUseCase`の完了を待たずに、再度`save()`を呼ぶ
  3. `UpdateReportStatusUseCase`（＝`PATCH /admin/reports/{id}/status`相当）が2回呼び出される
- 期待される結果: 他画面（`RoundJoinRequestListViewModel`等）と同様、処理中の二重タップ・二重呼び出しでは1回しかAPIが呼ばれないこと
- 実際の結果: `isUpdating`中でも`save()`が再実行され、`PATCH /admin/reports/{id}/status`相当のAPI呼び出しが重複しうる
- 影響: 通報管理画面はMVPでは「状態遷移順序の強制を行わない」設計（ADR-0007）であり、2回目の呼び出しが直ちにデータ不整合を起こすわけではないが、ネットワーク遅延時にボタン連打すると意図せず`handled_by_user_id`/`handled_at`が2回上書きされる、またはUIが最終的にどちらの呼び出し結果を表示するか不定になる可能性がある
- 差し戻し内容（提案）: `save()`冒頭に`if (state.isUpdating) return`を追加し、他画面のガードパターンと統一する

### 4-5. 【要確認・差し戻し】管理者向け一覧API `GET /admin/reports` のページネーション未実装

- 対象コード: `app/src/main/java/com/golfmatch/app/data/api/ApiService.kt`（`getAdminReports(status: String?)`）、`ReportRepository.getAdminReports(statusFilter: ReportStatus?)`、`GetAdminReportsUseCase`
- 2-6-3章E-2の判断のとおり、技術設計書6-9章は`before`/`limit`によるページネーションを明記しているが実装には存在しない。ArchitectAgent/DeveloperAgentにて「技術設計書どおりページネーションを実装する」か「MVPでは意図的に省略しADR-0007にその旨を追記する」かの決定を依頼したい

### 4-6. 【軽微・参考】MessageThreadScreenの自分/相手判定ロジックがViewModelではなくCompose Screen側にある

- 対象コード: `app/src/main/java/com/golfmatch/app/ui/screen/message/MessageThreadScreen.kt`の`MessageThreadContent`内、`MessageBubble(message = message, isMine = message.senderId != uiState.partnerId)`
- 依頼文の背景説明では「`MessageThreadViewModel`の自分/相手判定」という前提だったが、実際に確認したところ、この判定ロジック（`senderId != partnerId`でどちらの発言か判定）は`MessageThreadViewModel`ではなく`MessageThreadScreen`（`@Composable`関数）側に実装されていた。`MessageThreadViewModel`自体には分岐ロジックがほぼ無い（取得・送信の委譲のみ）。
- 動作上の不具合ではないが、0章のテスト方針（Compose UIテストは対象外、ViewModelはJVMユニットテスト対象）のもとでは、このロジックはCompose関数内にあるためJVMユニットテストの対象にできず（Robolectric等の追加基盤が必要）、実質的にテストカバレッジが無い状態になっている。ロジック自体は`senderId`と`partnerId`の単純な不一致判定であり複雑ではないため緊急度は低いが、将来的に`ViewModel`側で`isMine`を含む形式に変換してから`UiState`に渡す設計に変更すれば、JVMユニットテストの対象にできる（差し戻し必須ではない、設計改善の余地として記録）

---

## 5. 結論

現時点でテスト可能な範囲（domain/model, domain/usecase, data/mapper）について、技術設計書5章のデータモデル定義との齟齬は見つからなかった。UseCase層はいずれもRepositoryへの薄い委譲であり、レコメンドスコアリング等の主要ビジネスロジックはサーバー側実装待ちのためテスト対象外とした。

**2026-08-12追記（ADR-0006対応検証）**: コミット7f1b9c7（OTP検証への新規/既存ユーザー判定統合・`POST /auth/login`廃止）について、`./gradlew :app:assembleDebug`・`./gradlew :app:testDebugUnitTest`はいずれも成功（66件成功、0失敗）した。ADR-0006「実装への影響」表（55〜65行目）とコード変更（`AuthDto.kt`, `ApiService.kt`, `AuthSession.kt`, `AuthRepository.kt`, `VerifyPhoneOtpUseCase.kt`, `LoginUseCase.kt`削除, `AuthRepositoryImpl.kt`, `AuthMapper.kt`, `OtpVerificationViewModel.kt`）を1件ずつ照合し、不一致は見つからなかった。`docs/技術設計書.md`6-1章・11-2章もADR-0006と整合している。4-1章で報告していた「ログイン成功時にAuthSession.userIdが空文字列になる」バグは、`POST /auth/login`廃止と暗黙フォールバック廃止（ADR-0005の原則の引き継ぎ）により解消されたことをテストで確認し「解消済み」に更新した。新規のクリティカルなバグは発見しなかったが、`VerifyPhoneOtpUseCase`の`ExistingUser`分岐テストが欠落していたため追加した（2-5-2章AUTH-8）。また、認証フロー画面のViewModelテスト・Repository実装層テストの未整備を軽微な参考事項として記録した（4-3章）。

**2026-08-12追記（未実装だった残り全画面の実装検証、コミット590ebd9）**: `./gradlew :app:assembleDebug`・`./gradlew :app:testDebugUnitTest`はいずれも成功（104件成功、0失敗、既存66件＋新規38件）した。`ReportStatus`リネーム（ADR-0007）に伴う既存テストの追随修正について、アサーションが削られる形の弱化は無かったが、リネーム後4値中2値（`RESOLVED`/`DISMISSED`）が未検証、新規追加された管理者向けDTOマッパー・`User.isAdmin`が無テストという実質的なカバレッジ欠落を確認し補強した（2-6-1章）。新規追加された7 UseCase・複数ViewModelの分岐ロジック（主催者判定、承認/却下、多重操作防止ガード等）に対しテストを新規整備した（2-6-2章、`kotlinx-coroutines-test`を追加）。設計整合性レビューでは、DeveloperAgentからのエスカレーション3件のうち2件（画面ディレクトリ配置、管理者向けDTOの新規設計）は問題なしと判断し、1件（`GET /admin/reports`のページネーション未実装）は技術設計書6-9章との不整合として差し戻した（2-6-3章、4-5章）。加えて、新規に2件の指摘を記録した: (1)`ReportAdminDetailViewModel.save()`に他画面同様の多重操作防止ガードが欠落しているバグ（4-4章、再現テスト追加済み）、(2)`MessageThreadScreen`の自分/相手判定ロジックがViewModelではなくCompose Screen側にありJVMユニットテストの対象にできていない参考事項（4-6章）。
