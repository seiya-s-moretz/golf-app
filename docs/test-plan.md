# テスト計画：ゴルフマッチングアプリ（雛形・共通基盤フェーズ）

作成日: 2026-08-11
更新日: 2026-08-12（サーバーサイドPhase3（メッセージ・通報・ブロック・通報管理、`functions/`配下）の検証を反映、コミットc7d43ca。8章参照。**これによりサーバーサイド全フェーズの検証が完了**）
作成者: TesterAgent
対象: DeveloperAgent「プロジェクト雛形・共通基盤」フェーズ成果物、認証フロー画面実装（ADR-0006対応、コミット7f1b9c7）、未実装だった残り全画面（ラウンド新規作成/詳細/参加申請一覧、受信マッチング申請一覧、通報画面・ブロック済みユーザー一覧、メッセージ一覧/スレッド、通報管理簡易管理画面）の実装・フッター5タブ化（ADR-0001・ADR-0007対応、コミット590ebd9）、サーバーサイドPhase1実装（`functions/`配下、ADR-0008対応、コミット27426b3、および後続の修正コミット1770df3・cd26d55）、サーバーサイドPhase2実装（おすすめユーザー・マッチング申請・掲示板、`functions/`配下、コミットfa5f4cd）、およびサーバーサイドPhase3実装（メッセージ・通報・ブロック・通報管理、`functions/`配下、コミットc7d43ca）
参照元: `docs/要件定義書.md`（PRD、特に3-1章のレコメンドロジック、6章の懸念事項整理）, `docs/技術設計書.md`（技術設計、特に12章・13章・5章・6-1〜6-9章・10章）, `docs/adr/` 配下ADR（特に0001, 0003, 0006, 0007, 0008）, `README.md`, `functions/README.md`

**本書の構成**: 1〜5章はAndroidクライアント（`app/`配下）のJVMユニットテストを対象とする（雛形フェーズ〜コミット590ebd9まで）。**6章・7章・8章はサーバーサイド（`functions/`配下、Cloud Functions for Firebase）の検証**である。6章はPhase1（認証基盤・エリア・ユーザー・ラウンド募集）、7章はPhase2（おすすめユーザー・マッチング申請・掲示板）、8章はPhase3（メッセージ・通報・ブロック・通報管理）を対象とする。テスト対象・テスト手法が異なるため独立した章として追加した。

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

---

## 6. サーバーサイド（Cloud Functions for Firebase）テスト（2026-08-12追加、コミット27426b3検証）

本章は、DeveloperAgentが実装したサーバーサイドPhase1（`functions/`配下、TypeScript / Express / Firestore。認証基盤・エリアマスタ・ユーザー・ラウンド募集参加承認フロー）の検証結果である。**本プロジェクトでサーバーサイドコードを検証対象とするのは今回が初めて**であり、6-0章でテスト方針をサーバーサイド向けに新たに定義したうえで実施した。

### 6-0. テスト方針の拡張（サーバーサイド向け）

Androidクライアント側（1〜5章）はUseCase/Mapperの単体テストをFakeリポジトリで行ってきたが、サーバーサイドは以下の理由からこの方針をそのまま適用せず、**Firestore Emulatorに対する統合テスト**を採用した。

- `docs/技術設計書.md` 12-1章がサーバー側に「モック差し替えのためのリポジトリインターフェースを用意する必要性が薄い。実データストアに対してテストするほうが本番との乖離を防げる」という設計方針を明記しており（ADR-0008とも整合）、`service.ts`がFirestore Admin SDKを直接呼び出す構成上、単体テストのためにFirestoreをモックに差し替えることは同方針に反する
- Express製の単一HTTPS関数（`app.ts`の`createApp()`）はCloud Functionsの薄いラッパーに過ぎず、実質的な検証対象は「HTTPリクエスト→バリデーション（zod）→`service.ts`のビジネスロジック→Firestore→レスポンス」の一連の流れそのものであるため、supertestで`createApp()`を直接叩く統合テストの方が実装の妥当性を直接検証できる

導入したテストフレームワーク: **Jest + ts-jest + supertest + Firestore Emulator**（`@firebase/rules-unit-testing`もdevDependencyとして導入したが、今回のテストでは`firebase-admin`を直接使う既存の`src/config/firebaseAdmin.ts`の`db`をそのまま利用する方式を採ったため未使用。将来Firestoreセキュリティルール自体のテスト（現状は12-7章のとおり全拒否の単純なルールのため優先度低）を行う際に活用できる）。

- テストコード: `functions/test/`配下（`functions/src/**/*.test.ts`は使用せず、`src/`をプロダクションコードのみに保つため独立ディレクトリとした）
- 実行方法: `cd functions && npm test`（`package.json`に追加。内部で`firebase emulators:exec --only firestore ... "jest --runInBand"`を実行し、Firestore Emulatorの起動・テスト実行・終了までを一括で行う）
- `firebase-tools`は`^13.35.1`をdevDependencyとして固定した。理由: 検証環境のJavaが17系だが、2026-08時点最新の`firebase-tools`（15系）が要求するJavaは21以上のため、Java 17でも動作する13系を採用した（`functions/README.md`に注記済み）
- OTPコードの取得: `ConsoleSmsSender`が`firebase-functions/logger`経由でログ出力する内容を、`console.info`を横取りするテストヘルパー（`test/setup/consoleCapture.ts`）で読み取る方式とした。`functions/src`側の実装は一切変更していない
- 60秒レート制限・OTP有効期限切れ等の時間依存の境界値は、実時間を待つ代わりにFirestore Emulator上のドキュメントの`createdAt`/`expiresAt`を直接書き換えて再現した（`functions/src`側は変更せず、テストデータの前提条件のみを操作する一般的な統合テスト手法）

### 6-1. テスト対象範囲

依頼で優先度が高いとされた分岐ロジック・認可・バリデーションを持つAPIを対象とした。

| # | API | 対象ファイル |
|---|---|---|
| S-1 | `POST /auth/phone/otp` | `functions/test/auth/otp.test.ts` |
| S-2 | `POST /auth/phone/verify` | `functions/test/auth/verify.test.ts` |
| S-3 | `POST /users`（新規登録） | `functions/test/users/register.test.ts` |
| S-4 | `GET /areas` | `functions/test/areas/areas.test.ts` |
| S-5 | `GET /users/{id}` / `PUT /users/{id}` | `functions/test/users/getPut.test.ts` |
| S-6 | ラウンド参加申請・承認/却下フロー（`POST /round-events`, `GET/POST /round-events/{id}`, `POST /round-events/{id}/join-requests`, `GET .../join-requests`, `POST .../approve`, `POST .../reject`） | `functions/test/roundEvents/joinRequests.test.ts` |

対象外（Phase1未実装のため）: おすすめユーザー・マッチング申請（Phase2）、掲示板（Phase2）、メッセージ・通報・ブロック・通報管理（Phase3）。Twilioへの実SMS送信も対象外（`TwilioSmsSender`は未結線のスタブであり、`ConsoleSmsSender`のみをテスト対象とした。技術設計書12-5章の設計どおり）。

### 6-2. テストケースの要点

| # | 観点 | 主なケース |
|---|---|---|
| S-1 | OTP送信の60秒レート制限 | 初回204、60秒以内の再送信は429（旧OTP維持を確認）、60秒経過後は204で新OTP発行、電話番号ごとに独立、E.164形式バリデーション |
| S-2 | OTP検証・新規/既存判定 | OTP不一致400、未発行電話番号400、6桁以外の`otp_code`は400（zod）、未登録→`is_new_user=true`+`registration_token`、登録済み→`is_new_user=false`+`user`+`access_token`、試行5回超過でFAILED、有効期限切れ400、**【バグ再現】既存ユーザー分岐のレスポンス構造の契約違反（6-4章参照）** |
| S-3 | `POST /users` | 有効`registration_token`で201・`access_token`発行、発行後のトークンで認証必須APIを呼べる、不正/使い切り済み`registration_token`は401、`area_id`不正/非活性は400、`age`(0〜120)・`average_score`(40〜200)の境界値・範囲外 |
| S-4 | `GET /areas` | `is_active=true`のみ、`display_order`昇順、認証不要、0件時は空配列、レスポンス項目名の一致 |
| S-5 | `GET/PUT /users/{id}` | 未認証401、無効トークン401、本人取得200、他人取得200（**PII露出の懸念、6-4章参照**）、存在しないID404、PUTは本人以外403・未認証401・本人200・`age`/`average_score`の境界値・非活性/不正`area_id`400・不正`purpose`enum400 |
| S-6 | ラウンド参加申請フロー | 未認証401、`current=0`で作成、`capacity>current`検証（申請時・承認時の両方）、重複PENDING申請409、`GET .../join-requests`は主催者以外403、承認で`current`加算・`Connection`作成（`connections/{pairId}`を直接Getして確認）、主催者以外の承認/却下は403、二重承認防止409、却下後は`Connection`未作成・`current`加算なし |

### 6-3. 技術設計書・ADRとの整合性レビュー

`docs/技術設計書.md` 5章・6-1〜6-4章・12章・13章と`functions/src`配下の実装を1件ずつ照合した。エンドポイントのパス・メソッド・認証要否・バリデーション内容（年齢0〜120、スコア40〜200、E.164形式、OTP6桁等）・エラーレスポンス形式（`{ error: { code, message } }`）・Firestoreコレクション設計（12-2章のコレクション名・ドキュメントID戦略・複合インデックス）・認証ミドルウェア仕様（12-3〜12-4章）は、6-4章に記載する1件を除き矛盾は見つからなかった。

#### DeveloperAgentからのエスカレーション3件への判断

| # | エスカレーション事項 | TesterAgentの判断 |
|---|---|---|
| E-1 | `registration_token`を専用コレクションではなく`phoneVerifications`ドキュメントの内部拡張（`registrationTokenHash`/`registrationTokenExpiresAt`）として保存 | **問題なし**。技術設計書12-0章「前提1」が「クエリ簡略化・非正規化のための内部専用フィールドの追加は5章定義の変更にあたらない」と明記しており、本判断はこの前提の範囲内である。電話番号キーで管理されている既存ドキュメントに相乗りすることで、専用コレクションを新設した場合に必要になる追加のクエリ・複合インデックスも発生させておらず、実装上の合理性もある。`toUserResponse`等のAPIレスポンス生成コードで`registrationTokenHash`が漏洩していないことも確認した（`users.service.ts`の`UserResponse`に該当フィールドは存在しない）。ArchitectAgentへの差し戻しは不要だが、12-2-2章のコレクション一覧表に将来的に注記を追加しておくと次回以降のDeveloperAgent・TesterAgentが同じ疑問を持たずに済む（任意、緊急度低） |
| E-2 | `PUT /users/{id}`の認可を「本人のみ許可・403」として実装判断で追加 | **問題なし（むしろ必須の実装）**。技術設計書6-4章の他の書き込み系API（`.../approve`等）はいずれも「認可: `created_by`本人のみ」等を明記しており、`PUT /users/{id}`だけ認可要件が欠落しているのは6-3章側の記載漏れである可能性が高い。認可を実装しなかった場合、任意の認証済みユーザーが他人のプロフィールを書き換えられる重大な脆弱性になるため、「本人のみ許可」という判断は唯一の妥当な実装である。`functions/test/users/getPut.test.ts`で本人以外403・本人200を確認済み。ArchitectAgentへは差し戻し不要だが、技術設計書6-3章に「認可: 本人のみ」を明記する軽微な追記を推奨する |
| E-3 | `GET /round-events/{id}`（技術設計書6-4章に明記が無いが、Androidクライアント`ApiService.kt`が既に呼び出しているため追加実装） | **問題なし**。`app/src/main/java/com/golfmatch/app/data/api/ApiService.kt`を確認したところ、`@GET("round-events/{id}")`が実際に定義されており（`getRoundEvent()`）、DeveloperAgentの主張どおりクライアントは既にこのエンドポイントに依存している。実装しなければAndroid側のラウンド詳細画面が機能しないため、追加実装は正しい判断である。認証必須（`roundEventsRoutes.use(authenticate)`が全ルートに適用される）である点も他のround-events系APIと一貫しており問題ない。技術設計書6-4章への正式な追記をArchitectAgentに推奨する（機能的な問題はなく、ドキュメント整備の位置づけ） |

3件とも実装差し戻しは不要と判断した。ただし3件とも技術設計書側の記載漏れに起因するエスカレーションであるため、ArchitectAgentが6-3章・6-4章・12-2-2章の記載を実装に合わせて補完することを推奨する（機能追加ではなく既存実装の追認のための文書更新）。

### 6-4. 発見した不整合・バグ報告（DeveloperAgentへの差し戻し事項）

#### 6-4-1.【重大・要差し戻し】`POST /auth/phone/verify`の既存ユーザー分岐レスポンスがAndroidクライアントの契約と一致しない

- 対象コード: `functions/src/modules/auth/auth.service.ts`の`verifyPhoneOtp()`（`is_new_user=false`分岐、117〜121行目）
- 再現テスト: `functions/test/auth/verify.test.ts`の`【バグ再現】is_new_user=false時、user・access_tokenがsessionにネストされずトップレベルに返る（Androidクライアント契約違反）`（Pass=バグ挙動が現状のまま存在することを確認する目的）
- 詳細:
  - ADR-0006「実装への影響」表（`app/src/main/java/com/golfmatch/app/data/dto/AuthDto.kt`の行）は、`VerifyOtpResponseDto`が`session: AuthSessionResponseDto?`（`is_new_user=false`時のみ非null、`AuthSessionResponseDto`は`user`・`access_token`を持つ**ネストしたオブジェクト**）を持つと明記しており、実装済みのAndroidクライアント（`AuthDto.kt`）も実際にそのとおりの構造になっている
  - `AuthMapper.kt`の`VerifyOtpResponseDto.toDomain()`は`is_new_user=false`時に`checkNotNull(session)`でこの`session`フィールドの非null性を必須としている（`docs/test-plan.md`旧版2-5-2章AUTH-4も参照）
  - 一方、サーバー実装（`verifyPhoneOtp()`）は`user`・`access_token`を`session`にネストさせず、**トップレベルのフィールド**として返している（`{ is_new_user: false, user: {...}, access_token: "..." }`）
- 再現手順:
  1. 登録済みユーザーの電話番号でOTP発行・検証を行う
  2. サーバーのレスポンスは`{ is_new_user: false, user: {...}, access_token: "..." }`という平坦な構造で返る
  3. Androidクライアントがこれをパースすると、Gsonは存在しない`session`キーを`null`として扱うため、`AuthMapper.kt`の`checkNotNull(session)`が例外を送出する
- 期待される結果: `{ is_new_user: false, session: { user: {...}, access_token: "..." } }`という、`session`にネストした構造で返ること
- 実際の結果: `session`キー自体が存在せず、`user`・`access_token`がトップレベルに置かれている
- 影響: **既存ユーザーの再ログイン（`POST /auth/phone/verify`の`is_new_user=false`分岐）がAndroidクライアント側で必ず例外となり失敗する。** 新規登録直後の初回ログイン（`POST /users`のレスポンスは`AuthSessionResponseDto`をそのまま返す設計であり平坦な構造で一致するため問題なし）は影響を受けないが、アプリ再起動後の再ログイン等、既存ユーザーとして`verify`を通るケース全てが影響を受ける、認証基盤の中核に関わる重大度の高い不具合
- 差し戻し内容: `verifyPhoneOtp()`の`is_new_user=false`時のレスポンスを`{ is_new_user: false, session: { user, access_token } }`という構造に修正する必要がある。技術設計書6-1章の文言自体（「`user`、`access_token`」という記載）はネスト構造を明示していないため、あわせてADR-0006の内容を正として6-1章の表現を明確化する追記をArchitectAgentに依頼することを推奨する

#### 6-4-2.【要確認・PII露出の懸念】`GET /users/{id}`が他人閲覧時にも生の電話番号(`phone_number`)を返す

- 対象コード: `functions/src/modules/users/users.service.ts`の`toUserResponse()`
- 再現テスト: `functions/test/users/getPut.test.ts`の`【要確認】他人のプロフィール取得時にもphone_number（生の電話番号）が含まれる`
- 詳細: 技術設計書6-3章の`GET /users/{id}`の記述は「レスポンスに`area`（AreaMasterの参照展開）, `phone_verified`を追加」という差分表現のみであり、`phone_number`自体を追加するとは明記していない。一方5-1章のUser Entity定義は`phone_number`を新規フィールドとして挙げているため、実装（Userの論理モデル全体を返す）は12-0章「前提1」の解釈次第では矛盾しないとも言える。いずれの解釈が正しいにせよ、Androidクライアントの`GetUserUseCase`は`BoardViewModel`・`MessageThreadViewModel`から「他人」のプロフィール取得（掲示板投稿者・メッセージ相手）にも使われている（`GET /users/{id}`は本人限定ではなく認証済みなら誰でも呼べる、6-4-3参照）ため、実運用では任意の認証済みユーザーが他ユーザーの生電話番号を閲覧できてしまう
- 実装はバグではなく、技術設計書の記述をどちらの解釈で読むかに依存する設計レベルの論点である。ただしPRD 3-1章の本人確認（SMS OTP）は「なりすまし・Bot登録の抑止」が目的であり、電話番号を他ユーザーに公開する意図は読み取れないため、ArchitectAgentに「`GET /users/{id}`のレスポンスから`phone_number`を除外する（本人が自分の情報を確認する用途は別途`GET /users/me`相当の設計を検討する、または`phone_verified`のみで足りるかを再確認する）」方向での再検討を推奨する
- 差し戻し内容（提案、決定はArchitectAgent/DeveloperAgentに委ねる）: `phone_number`をレスポンスから外す、または閲覧者が本人の場合のみ含める等の設計判断を仰ぎたい

#### 6-4-3.【軽微・参考】`GET /users/{id}`に本人限定の認可が無いこと自体は技術設計書の記述通り（バグではない）

- 6-3章E-2で判断したとおり`PUT /users/{id}`（更新系）は本人限定にすべきだが、`GET /users/{id}`（閲覧系）はそもそも掲示板投稿者・ラウンド参加者等「他人」のプロフィールを見るために使われる設計であり、本人限定にしないこと自体は妥当。6-4-2章のPII露出懸念とは別の論点として記録する（差し戻し不要）

#### 6-4-4.【環境依存・参考】Windows環境でのFirestore Emulatorプロセス残留（プロダクトのバグではない）

- `firebase emulators:exec`がCLIログ上「正常終了」を報告した後も、Firestore Emulatorの子プロセス（`java.exe`）がOSプロセスとして残留し、次回のテスト実行時に`Port 8080 is not open`エラーで失敗することが本検証中に複数回発生した。`firebase-tools`のWindows環境におけるシグナルハンドリングに起因すると見られる既知の環境依存の癖であり、`functions/src`側の実装とは無関係である。`functions/README.md`に対処法（残留プロセスの特定・終了）を注記した。CI環境（Linux）ではこの問題は発生しない可能性が高いが、CI導入時は要観察

### 6-5. テスト実行結果サマリ

- 実行コマンド: `cd functions && npm test`（`firebase emulators:exec --only firestore ... "jest --runInBand"`）
- 実行環境: Node.js v22.16.0 / Java 17（`firebase-tools@13.35.1`に固定。6-0章参照）
- テストスイート: **6ファイル全て成功**
- テストケース: **63件実行、成功63件、失敗0件**（クリーンな環境で2回連続実行し安定して63件成功することを確認。内訳は6-2章の表に対応する各テストファイル: `otp.test.ts` 7件、`verify.test.ts` 8件（うちバグ再現1件）、`register.test.ts` 13件、`areas.test.ts` 5件、`getPut.test.ts` 14件（うちPII露出確認1件）、`joinRequests.test.ts` 15件）
- ビルド・Lint: `npm run build`（`tsc`）・`npm run lint`（`eslint src --ext .ts`）はいずれも成功（テストコード追加による`src`配下への影響が無いことを確認）
- リグレッション: 本検証はサーバーサイドの新規テスト整備であり、Androidクライアント側（1〜5章）の既存テストへの影響はない（`app/`配下は変更していない）

### 6-6. まとめ

サーバーサイドPhase1実装について、6章のAPI仕様・5章のデータモデル・ADR-0001/0003/0006/0007/0008との整合性を63件のFirestore Emulator統合テストで検証した。DeveloperAgentからのエスカレーション3件はいずれも実装差し戻し不要と判断したが、対応する技術設計書側の記載漏れ（6-3章の認可要件、6-4章の`GET /round-events/{id}`、12-2-2章の`registration_token`保存場所）をArchitectAgentが補完することを推奨する。新たに、認証基盤の中核に影響する重大な契約違反バグ（`POST /auth/phone/verify`の既存ユーザー分岐レスポンスが`session`にネストされていない、6-4-1章）を発見し、DeveloperAgentへの差し戻し事項として記録した。また、PII露出の懸念（`GET /users/{id}`が他人閲覧時にも生の電話番号を返す、6-4-2章）を設計レベルの要確認事項としてArchitectAgentに提起した。

**2026-08-12追記（Phase1バグ修正の反映確認）**: 6-4-1章のバグはコミット1770df3「fix: verify OTPレスポンスのsessionネスト化と他人閲覧時のphone_number非露出」で修正済みであることを、既存テスト`verify.test.ts`が引き続き成功していること（本更新時点のテスト実行結果は7-5章参照）で確認した。同コミットでは6-4-2章のPII露出懸念にも対応し、`toUserResponse()`が閲覧者本人の場合のみ`phone_number`を含めるよう修正されている（`getPut.test.ts`の該当テストが更新され成功していることを確認済み、7章の回帰確認の一部）。いずれも本章時点（6章）のテストコードは修正不要であり、DeveloperAgent側で追随修正されたテストがそのまま成功する形になっている。

---

## 7. サーバーサイドPhase2（おすすめユーザー・マッチング申請・掲示板）テスト（2026-08-12追加、コミットfa5f4cd検証）

本章は、DeveloperAgentが実装したサーバーサイドPhase2（`functions/src/modules/matching/`・`functions/src/modules/board/`。おすすめユーザー・マッチング申請・掲示板）の検証結果である。テスト方針・テスト基盤（Jest + ts-jest + supertest + Firestore Emulator）は6-0章と同一のものを踏襲した。

### 7-1. テスト対象範囲

| # | API | 対象ファイル |
|---|---|---|
| P-1 | `GET /users/recommend` | `functions/test/matching/recommend.test.ts` |
| P-2 | `POST /users/{id}/match-requests` | `functions/test/matching/matchRequests.test.ts` |
| P-3 | `GET /users/me/match-requests?direction=received\|sent` | `functions/test/matching/matchRequests.test.ts` |
| P-4 | `POST /match-requests/{id}/approve` / `reject` | `functions/test/matching/matchRequests.test.ts` |
| P-5 | `GET /board` / `POST /board` | `functions/test/board/board.test.ts` |
| P-6 | `/users`プレフィックス共有によるルーティング回帰確認（`usersMatchingRoutes`と`usersRoutes`） | `functions/test/matching/usersRouting.test.ts` |

対象外（Phase3未実装のため）: ブロック関係によるおすすめユーザー・掲示板からの除外フィルタ（6-5・6-6章に明記されているが、コミットメッセージ・`matching.service.ts`/`board.service.ts`のコメントに「ブロックによる除外はPhase3で後付け改修する方針」と明記されており、13-3章の依存関係を踏まえた計画的な未実装であることをコードで確認した。バグとしては扱わない）。メッセージ・通報・通報管理（Phase3）も引き続き対象外。

### 7-2. テストケースの要点

| # | 観点 | 主なケース |
|---|---|---|
| P-1 | レコメンドスコアリング境界値 | 未認証401、自分自身が結果に含まれない、スコア差=10(閾値内、40点)と=11(閾値外、0点)の境界、合計スコア=40点(未推薦)と=60点(推薦)の境界、全一致100点・不一致0点、スコア降順ソート |
| P-2 | マッチング申請作成 | 未認証401、自分自身への申請400、存在しない宛先404、正常作成201、同一方向PENDING重複409、**逆方向は独立してPENDING併存しうる（設計確認、後述7-3）**、処理済み後の再申請は409にならない |
| P-3 | 方向別一覧 | direction不正値/未指定は400（zod enum必須）、received/sentの絞り込みが正しいユーザー・件数を返す |
| P-4 | 承認/却下の認可 | 宛先(`to_user_id`)本人以外403（申請者本人による自己承認も403）、宛先本人の承認200+Connection作成確認、却下200+Connection未作成確認、処理済み申請への再承認409、存在しないID404 |
| P-5 | 掲示板 | 未認証401（GET/POST）、投稿作成201の全項目確認、content空文字400、全件を`created_at`降順で返す、0件時は空配列 |
| P-6 | ルーティング回帰 | `POST /users`（未認証・新規登録）が401にならず201になること、`GET/PUT /users/{id}`が`usersMatchingRoutes`に飲み込まれず`usersRoutes`にフォールスルーして正常応答すること、`GET /users/recommend`自体は未認証401になること、`GET /users/me/match-requests`が`GET /users/:id`（`id="me"`）として誤解釈されないこと |

境界値の設計上の注記: レコメンドの各加点（スコア差40点／エリア一致40点／目的一致20点）はいずれも20の倍数であり、到達しうる合計スコアは{0,20,40,60,80,100}のみで「ちょうど59点」は原理上存在しない。依頼にあった「合計スコアちょうど60・59」の境界検証は、実質的な境界である「推薦される最小値=60点」と「推薦されない最大値=40点」として実施した（`recommend.test.ts`冒頭コメントに明記）。

### 7-3. DeveloperAgentの実装判断4点への評価

依頼にあった4点の実装判断を、技術設計書・要件定義書と照合したうえで評価した。

| # | 実装判断 | TesterAgentの評価 |
|---|---|---|
| 1 | レコメンド結果をスコア降順でソート | **問題なし**。技術設計書6-5章に順序の明記は無く、要件定義書3-1章も「60点以上で推薦」という閾値のみを規定し順序には触れていない。推薦の趣旨（スコアの高いユーザーほど優先的に見せる）に照らして降順ソートは合理的な判断であり、`recommend.test.ts`でソート順を確認した。逆順・ソート無しを積極的に要求する記述はどこにも無い |
| 2 | マッチング申請の重複防止が方向別(`from_user_id`+`to_user_id`+`PENDING`)のみで逆方向は非チェック | **技術設計書の記載どおりであり実装はバグではないが、UX上の考慮不足の可能性ありとして要確認事項に記録する**。技術設計書5-2章のMatchRequestモデル定義は「`(from_user_id, to_user_id)` の組み合わせでPENDING状態は1件まで（重複申請防止）」と、あくまで方向付きの組(ordered pair)として制約を明記しており、実装（`matching.service.ts`の`createMatchRequest`）はこの文言に忠実である。したがって技術設計書との不整合ではない。一方、要件定義書3-1章・技術設計書6-5章のいずれにも「双方向から同時に申請が来た場合の挙動（自動マッチ扱いにする等）」の規定が無いため、AさんがBさんに申請している最中に、Bさんも独立してAさんに申請できてしまう（`matchRequests.test.ts`の「【設計確認】逆方向(to→from)の申請は独立して扱われ...」で現状挙動を確認済み）。承認処理自体はConnectionが冪等生成（`ensureConnection`）のため技術的な実害はないが、Androidクライアント側UIが「相手から既に申請が来ている」ことを考慮せずマッチング申請ボタンを表示し続ける可能性があり、ArchitectAgent/ProductManagerAgentに「双方向PENDING併存を許容する仕様でよいか」の確認を推奨する（差し戻しではなく設計確認事項） |
| 3 | `GET /board`はページネーション無しで全件取得 | **問題なし**。技術設計書6-6章は`GET /board`を「既存踏襲」とのみ記載しページネーションに触れておらず、Androidクライアント`ApiService.getBoardPosts()`（今回のタスク範囲外のため確認のみ、変更はしていない）も引数を取らないことをコードで確認した。6-9章の管理者向け一覧APIのように明示的にページネーションが規定されているAPIとは異なり、`GET /board`について技術設計書側に矛盾する記載は無い。将来的に投稿数が増えた場合のパフォーマンス懸念は残るが、MVPの範囲としては妥当と判断する |
| 4 | `usersMatchingRoutes`をルーター単位でなくルート単位で`authenticate`を付与 | **正しく機能していることを確認した**。仮に`usersMatchingRoutes.use(authenticate)`としていた場合、`usersMatchingRoutes`が`/users`に`usersRoutes`より先にマウントされる構成上、認証不要な`POST /users`（新規登録）へのリクエストもこのルーターを経由する際に認証チェックが先に走り401になってしまう。`usersRouting.test.ts`で（a）`POST /users`が未認証のまま201で成功すること、（b）`GET/PUT /users/{id}`が`usersMatchingRoutes`に飲み込まれず`usersRoutes`までフォールスルーして正常応答すること、（c）`GET /users/recommend`自体は未認証で401になること、の3点を実地に確認し、いずれも設計判断どおりに機能していた |

### 7-4. 発見した不整合・バグ報告

本Phase2実装について、明確なバグ（技術設計書・要件定義書との不整合で修正が必要なもの）は見つからなかった。7-3章の#2（マッチング申請の逆方向チェック非実装）のみ、バグではなく仕様確認事項として記録する（差し戻し必須ではない、ArchitectAgent/ProductManagerAgentへの確認を推奨）。

### 7-5. テスト実行結果サマリ

- 実行コマンド: `cd functions && npm test`（6-0章と同一。実行前にWindows環境特有のFirestore Emulator残留プロセスの終了が必要な場合がある、6-4-4章参照）
- 実行環境: Node.js v22.16.0 / Java 17（`firebase-tools@13.35.1`固定、変更なし）
- テストスイート: **10ファイル全て成功**（Phase1の6ファイル＋Phase2の新規4ファイル）
- テストケース: **102件実行、成功102件、失敗0件**。`--json`出力（`jest --runInBand --json`）で1件ずつ正確にカウントした内訳は次のとおり: Phase1（変更なし・既存6ファイル、計64件）— `otp.test.ts` 7件、`verify.test.ts` 8件、`register.test.ts` 13件、`areas.test.ts` 5件、`getPut.test.ts` 16件、`joinRequests.test.ts` 15件／Phase2（新規4ファイル、計38件）— `recommend.test.ts` 9件、`matchRequests.test.ts` 18件、`board.test.ts` 6件、`usersRouting.test.ts` 5件
  - 注記: 6-5章に記載の「Phase1 63件」は`test.each`のパラメータ展開を考慮しない目視カウントによる概数だった（実際は`getPut.test.ts`の`test.each`2ブロック分の展開により64件が正確な件数）。6-5章の記載自体は本タスクの範囲外のため修正していないが、本章では`--json`出力による正確な件数を採用した
- **Phase1回帰確認**: `otp.test.ts`・`verify.test.ts`・`register.test.ts`・`areas.test.ts`・`getPut.test.ts`・`joinRequests.test.ts`の既存64件は変更を一切加えておらず、全件成功した。特に依頼で重点確認を求められた`POST /users`・`GET/PUT /users/{id}`（`usersMatchingRoutes`との`/users`プレフィックス共有によるルーティング変更の影響を受けうる箇所）は、既存テスト64件中の該当分がいずれも成功したことに加え、7-1章P-6の新規回帰専用テスト（`usersRouting.test.ts`）でも重点的に確認し、問題は見つからなかった
- ビルド・Lint: `npm run build`（`tsc`）・`npm run lint`（`eslint src --ext .ts`）はいずれも成功
- リグレッション: 上記のとおりPhase1・Androidクライアント側（1〜5章）ともに影響なし（`app/`配下は変更していない）

### 7-6. まとめ

サーバーサイドPhase2実装（おすすめユーザー・マッチング申請・掲示板）について、技術設計書6-5章・6-6章、要件定義書3-1章のレコメンドロジックとの整合性を38件の新規Firestore Emulator統合テスト（既存64件と合わせ計102件）で検証した。依頼にあった4点の実装判断（レコメンド降順ソート、マッチング申請重複防止の方向別チェック、掲示板ページネーション省略、`/users`プレフィックス共有時のルート単位認証）は、いずれも技術設計書・要件定義書との矛盾は無く、うち1点（マッチング申請の逆方向重複非チェック）のみ、技術設計書の文言自体には忠実であるものの双方向PENDING併存というUX上の考慮点をArchitectAgent/ProductManagerAgentへの確認事項として記録した。新たなバグは発見されず、Phase1の既存63件のテストも全て成功しリグレッションは確認されなかった。

---

## 8. サーバーサイドPhase3（メッセージ・通報・ブロック・通報管理）テスト（2026-08-12追加、コミットc7d43ca検証）

本章は、DeveloperAgentが実装したサーバーサイドPhase3（`functions/src/modules/messaging/`・`functions/src/modules/reports/`（`admin/`サブディレクトリ含む）・`functions/src/modules/blocks/`。既存の`roundEvents/`・`matching/`・`board/`へのブロック除外・拒否ロジック追加改修を含む）の検証結果である。テスト方針・テスト基盤（Jest + ts-jest + supertest + Firestore Emulator）は6-0章と同一のものを踏襲した。**本Phaseの完了により、技術設計書13-2章で計画されたサーバーサイド全フェーズ（Phase1〜3）の実装・検証が完了する。**

### 8-1. テスト対象範囲

| # | API/機能 | 対象ファイル |
|---|---|---|
| M-1 | `GET /conversations`・`GET/POST /conversations/{partnerId}/messages`・`POST /conversations/{partnerId}/read` | `functions/test/messaging/messaging.test.ts` |
| M-2 | `POST/DELETE /users/{id}/block`・`GET /users/me/blocks`（冪等性含む） | `functions/test/blocks/blocks.test.ts` |
| M-3 | `POST /reports` | `functions/test/reports/reports.test.ts` |
| M-4 | `GET /admin/reports`・`GET /admin/reports/{id}`・`PATCH /admin/reports/{id}/status` | `functions/test/reports/adminReports.test.ts` |
| M-5 | ブロックによる遡及フィルタ（`GET /round-events`・`GET /users/recommend`・`GET /board`からの除外、`POST /users/{id}/match-requests`・`POST /round-events/{id}/join-requests`のブロック時拒否） | `functions/test/blocks/blockFiltering.test.ts` |

### 8-2. テストケースの要点

| # | 観点 | 主なケース |
|---|---|---|
| M-1 | メッセージ送受信 | 未認証401、Connectionが無い相手への送信403 FORBIDDEN、自分自身への送信400、存在しない相手への送信404、`content`空文字400、`content`500文字ちょうどOK/501文字400（境界値）、ブロック関係での送信拒否403 BLOCKED（自分が相手をブロック／相手が自分をブロックの両方向）、`GET .../messages`はConnection無しで403、`created_at`降順、`limit`/`before`によるページネーション、既読化で`read_at`・`unread_count`が更新される、非正規化フィールド（`lastMessageId`等）が複数往復のやり取り後も双方の視点で正しい |
| M-2 | ブロック | 未認証401、自分自身のブロック400、存在しないユーザーのブロック404、正常ブロック204→一覧反映、重複ブロックの冪等性（エラーにならず一覧は1件のまま）、未ブロックの相手への解除リクエストも204（冪等）、解除の二重実行も204（冪等）、`GET /users/me/blocks`は自分がブロックした相手のみを返す（相手からブロックされているだけの相手は含まない） |
| M-3 | 通報作成 | 未認証401、USER/BOARD_POST両対象での作成、`reason_category=OTHER`時`reason_text`未指定/空白のみは400、指定時は201、OTHER以外は`reason_text`省略可、不正な`target_type`/`reason_category`は400、存在しない通報対象は404 |
| M-4 | 通報管理(admin) | `is_admin=false`で3エンドポイントとも403、一覧は`created_at`降順・`status`絞り込み・`limit`/`before`ページネーション・0件時空配列、詳細はUSER対象で`target_detail.user`に`phone_number`キーが含まれないことをオブジェクトのプロパティ検査とJSON文字列検査の両方で確認、BOARD_POST対象は`target_detail.board_post`のみ非null、ステータス更新で`handled_by_user_id`/`handled_at`が呼び出し元管理者で反映、`handling_memo`未指定時は既存値を保持、状態遷移順序は強制されない（逆行も許可、ADR-0007）、不正な`status`値・存在しないIDは400/404 |
| M-5 | ブロックの遡及フィルタ | `round-events`一覧・`recommend`・`board`のいずれも、自分が相手をブロックした場合に加え、**相手から一方的にブロックされているだけの場合も除外される（双方向）**ことを確認。`match-requests`・`round-events`の参加申請は、いずれの方向のブロックでも403 BLOCKEDを返すことを確認 |

### 8-3. DeveloperAgentの実装判断5点への評価

依頼にあった5点の実装判断を、技術設計書・要件定義書・ADR-0007と照合したうえで評価した。

| # | 実装判断 | TesterAgentの評価 |
|---|---|---|
| 1 | ブロック除外の方向性: `round-events`一覧・`recommend`・`board`の3APIすべてで双方向ブロック（自分が相手をブロック、または相手から自分がブロックされている、のいずれか）による除外を実装 | **要確認だが差し戻し不要。技術設計書側の表現の精緻化をArchitectAgentに推奨する。** 技術設計書5-2章Block「効果」一覧（プロダクトオーナー確認済み、10章#1参照）は、`recommend`は「ブロック関係（**双方向**）にあるユーザーを結果から除外」と明記する一方、`board`は「ブロックした**ユーザーの投稿**を除外」という能動的な表現で「双方向」の明記が無く、`round-events`は「ブロック関係にある募集作成者の募集を結果から除外」と中間的な表現になっている。文言だけを厳密に読むと、`board`のみ片方向（自分が能動的にブロックした相手の投稿のみ除外）を意図しているようにも読める。一方、より実装に近い12-2-3章は「一覧系API（`GET /round-events`, `GET /users/recommend`, `GET /board`）のブロック除外フィルタは...`blocker_user_id==me`・`blocked_user_id==me`の2クエリのUnion」と3APIを区別せず同一の双方向ロジックで一般化しており、12-4章も`excludeBlockedUsers`という共通関数として扱っている。加えて、5-2章の効果一覧の他項目（マッチング申請・ラウンド参加申請・メッセージ送信の拒否）は「ブロック関係にある**ユーザー間**」という一貫した双方向表現であり、`board`だけを意図的に非対称にする合理的理由は5-2章の文言からは読み取れない。安全機能としてのブロックは「自分をブロックした相手のコンテンツも見せない」という対称設計の方が一般的なブロック機能の直感にも合致する。以上を踏まえ、DeveloperAgentの双方向統一実装は12-2-3章の実装レベルの記述に忠実であり、5-2章の`board`項目の文言が精緻さを欠いていた可能性が高いと判断し、**実装の差し戻しは行わない**。ただし5-2章の文言とプロダクトオーナー確認済み事項（10章#1）が「`board`除外の方向性」を明示的に確認したとまでは言い切れないため、5-2章の`board`項目の表現を12-2-3章と整合するよう「ブロック関係（双方向）にあるユーザーの投稿を除外」に修正することをArchitectAgentに推奨する（表現の精緻化であり機能修正ではない。`functions/test/blocks/blockFiltering.test.ts`冒頭コメントに詳細な判断根拠を記録済み） |
| 2 | メッセージ最大文字数500文字（設計書に明記が無いための実装判断） | **問題なし**。技術設計書5-2章のMessageモデル定義・6-7章のいずれにも具体的な文字数上限の明記が無く、矛盾する記載も無い。500文字は一般的なチャットメッセージの上限として不合理ではなく、`MESSAGE_MAX_LENGTH`定数として一箇所に定義されている実装も妥当。境界値（500文字OK・501文字400）をテストで確認した |
| 3 | `POST/DELETE /users/{id}/block`が冪等実装（重複ブロック・存在しないブロック解除もエラーにしない） | **問題なし**。技術設計書6-3章・6-8章に重複時のエラー要件の明記は無く、ドキュメントID戦略（`blocker_user_id_blocked_user_id`固定、12-2-1章）を踏まえると`set()`による上書き・存在しない`delete()`の許容はFirestoreの自然な実装であり、UX上も「再ブロックがエラーになる」必然性は無い。他の一覧系APIが0件時に空配列を返す（エラーにしない）設計とも一貫している |
| 4 | `GET /conversations`の未読件数・最新メッセージプレビューをConnectionドキュメントへの非正規化フィールドで実現 | **問題なし。整合性も確認できた**。`sendMessage()`はFirestoreトランザクション内で(1)メッセージ作成、(2)`connections`の非正規化フィールド（`lastMessageId`/`lastMessagePreview`/`lastMessageAt`/`lastMessageSenderId`/`unreadCountForUser*`）更新を同時に行っており、部分的な不整合が生じる余地がない設計になっている。`messaging.test.ts`の「複数往復のやり取り後もlast_message・unread_countが双方の視点で正しい」で、A→B→Aと3通やり取りした後の`GET /conversations`が双方の視点で正しい値を返すことを確認した |
| 5 | `POST /conversations/{partnerId}/read`の既読判定を`pairId`単一条件取得後にアプリ側フィルタで実現 | **MVP規模では許容範囲と判断する**。技術設計書12-2-3章が既にブロック除外フィルタについて「複合インデックスの追加を避けるためアプリケーションコード側でフィルタする」という同種の設計判断をMVP規模の前提で明記しており、本実装（`markConversationRead()`のコメントが同じ理由付けを踏襲）はこの既存の設計方針の範囲内である。ただし、この方式は会話が長く続くほど（未読分だけでなく）会話全体のメッセージを毎回全件取得するため、Firestoreの読み取りコスト・レイテンシが会話の総メッセージ数に比例して増加する点は、ブロック関係（ユーザーあたり件数が少ない想定）とは性質が異なり、将来的なスケール時には未読メッセージ用の複合インデックス（`pairId`+`senderId`+`readAt`等）の追加を検討する余地がある（差し戻し不要、将来検討事項として記録） |

### 8-4. 発見した不整合・バグ報告

本Phase3実装について、明確なバグ（技術設計書・要件定義書との不整合で修正が必要なもの）は見つからなかった。8-3章#1（ブロック除外の方向性）のみ、実装の差し戻しは不要と判断したが、技術設計書5-2章`board`項目の表現精緻化をArchitectAgentに推奨する設計確認事項として記録する。

### 8-5. テスト実行結果サマリ

- 実行コマンド: `cd functions && npm test`（6-0章と同一。実行前にWindows環境特有のFirestore Emulator残留プロセスの終了が必要な場合がある、6-4-4章参照。本検証中も1回発生し対処した）
- 実行環境: Node.js v22.16.0 / Java 17（`firebase-tools@13.35.1`固定、変更なし）
- テストスイート: **15ファイル全て成功**（Phase1の6ファイル＋Phase2の4ファイル＋Phase3の新規5ファイル）
- テストケース: **173件実行、成功173件、失敗0件**。`--json`出力で正確にカウントした内訳: Phase1（既存6ファイル、変更なし、計64件）／Phase2（既存4ファイル、変更なし、計38件）／Phase3（新規5ファイル、計71件）— `messaging.test.ts` 20件、`adminReports.test.ts` 17件、`blocks.test.ts` 12件、`blockFiltering.test.ts` 11件、`reports.test.ts` 11件
- **Phase1・Phase2回帰確認**: 既存10ファイル（Phase1 6ファイル・Phase2 4ファイル）は変更を一切加えておらず、既存102件（64+38）が全件成功した。特に本Phaseで`roundEvents`・`matching`・`board`の各`service.ts`にブロック除外ロジックが追加改修されたため、Phase1の`joinRequests.test.ts`（15件）・Phase2の`recommend.test.ts`（9件）・`matchRequests.test.ts`（18件）・`board.test.ts`（6件）に対する影響を重点的に確認したが、いずれも全件成功しリグレッションは無かった
- ビルド・Lint: `npm run build`（`tsc`）・`npm run lint`（`eslint src --ext .ts`）はいずれも成功
- リグレッション: Androidクライアント側（1〜5章）・Phase1（6章）・Phase2（7章）ともに影響なし（`app/`配下は変更していない、Phase1・Phase2のテストファイルも変更していない）

### 8-6. まとめ

サーバーサイドPhase3実装（メッセージ・通報・ブロック・通報管理）について、技術設計書6-7章〜6-9章・5-2章、ADR-0007との整合性を71件の新規Firestore Emulator統合テスト（既存102件と合わせ計173件）で検証した。依頼にあった5点の実装判断のうち4点（メッセージ最大文字数、ブロックの冪等実装、Connection非正規化フィールドの整合性、既読判定のアプリ側フィルタ）は技術設計書・ADRとの矛盾は無く妥当と判断した。残る1点（ブロック除外の双方向統一実装）は、技術設計書5-2章の`board`項目の文言（片方向的表現）と12-2-3章の実装レベルの記述（3API共通の双方向ロジック）の間に表現上の緊張関係があったが、12-2-3章の一般化された記述・他項目との一貫性・安全機能としての合理性を踏まえ、実装の差し戻しは不要と判断した。ただし技術設計書側の文言の精緻化をArchitectAgentへの確認事項として記録した（8-3章#1）。新たなバグは発見されず、Phase1・Phase2の既存102件のテストも全て成功しリグレッションは確認されなかった。

---

## 9. サーバーサイド全フェーズ（Phase1〜3）を通したテスト結果サマリ

本Phase3の検証をもって、技術設計書13-2章で計画されたサーバーサイド（Cloud Functions for Firebase、`functions/`配下）の実装・検証が全フェーズ完了した。

| Phase | 対象 | コミット | テストファイル数 | テストケース数 | 検証結果 |
|---|---|---|---|---|---|
| Phase1 | 認証基盤・エリアマスタ・ユーザー・ラウンド募集参加承認フロー | 27426b3（+修正1770df3・cd26d55） | 6 | 64 | 全件成功。重大バグ1件発見・修正確認済み（6-4-1章、既存ユーザー分岐レスポンスの`session`ネスト漏れ）、PII露出懸念1件発見・修正確認済み（6-4-2章） |
| Phase2 | おすすめユーザー・マッチング申請・掲示板 | fa5f4cd | 4 | 38 | 全件成功。バグ無し。設計確認事項1件（マッチング申請の逆方向重複非チェック、7-3章#2） |
| Phase3 | メッセージ・通報・ブロック・通報管理 | c7d43ca | 5 | 71 | 全件成功。バグ無し。設計確認事項1件（ブロック除外の方向性、8-3章#1） |
| **合計** | — | — | **15** | **173** | **全件成功、失敗0件** |

**未解決の差し戻し事項（サーバーサイド、`functions/src`配下）**: 現時点で**無し**。過去に発見された重大バグ（6-4-1章）・PII露出懸念（6-4-2章）はいずれもコミット1770df3で修正され、修正後のテストも成功していることを確認済みである。Phase2・Phase3で記録した2件の設計確認事項（マッチング申請の逆方向重複、ブロック除外の`board`項目の文言精緻化）は、いずれも現在の実装が技術設計書の文言と矛盾するとまでは言えない「確認・推奨」レベルの事項であり、機能上の不具合ではないため、実装の差し戻しは発生していない。

なお、サーバーサイドとは別に、Androidクライアント側（1〜5章）で発見した未解決の差し戻し事項が1件残っている（4-4章「`ReportAdminDetailViewModel.save()`に多重操作防止ガードが無い」、コミット590ebd9検証時発見）。本タスクはサーバーサイドの検証範囲であり`app/`配下には触れていないため、この事項の状態は変わっていない。また、4-5章「`GET /admin/reports`のページネーション未実装」については、Androidクライアント側のUseCase/Repository/ApiServiceの`before`/`limit`引数対応状況は本タスクの範囲外（`app/`配下）だが、参考情報として、サーバーサイド側の`GET /admin/reports`は本Phase3実装で`before`/`limit`によるページネーションに対応済みであることを確認した（8-1章M-4、8-2章）。Androidクライアント側の追随実装状況は別途確認が必要である。
