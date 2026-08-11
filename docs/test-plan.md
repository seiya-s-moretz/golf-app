# テスト計画：ゴルフマッチングアプリ（雛形・共通基盤フェーズ）

作成日: 2026-08-11
作成者: TesterAgent
対象: DeveloperAgent「プロジェクト雛形・共通基盤」フェーズ成果物
参照元: `docs/要件定義書.md`（PRD）, `docs/技術設計書.md`（技術設計）, `docs/adr/` 配下ADR, `README.md`

---

## 0. テスト対象範囲

現時点では画面UI（Compose Screen/Container/ViewModel）は未実装（次フェーズ）のため、UIテストは対象外とする。
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
| U-7 | RequestPhoneOtpUseCase / VerifyPhoneOtpUseCase / RegisterUserUseCase | 委譲 |
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
| M-10 | AuthMapper | `VerifyOtpResponseDto`→`RegistrationToken`、`AuthSessionResponseDto`→`AuthSession`（下記4章のバグ報告参照） |

テストコード: `app/src/test/java/com/golfmatch/app/data/mapper/*.kt`

---

## 3. テスト実行結果サマリ

- 実行コマンド: `./gradlew :app:assembleDebug` / `./gradlew :app:testDebugUnitTest`
- ビルド: 成功
- ユニットテスト: 62件実行、成功62件、失敗0件、エラー0件
- テストファイル数: 18ファイル（domain/model 2、data/mapper 9、domain/usecase 6、testutil（Fixture/Fake）2）

---

## 4. 発見した不整合・バグ報告（DeveloperAgentへの差し戻し事項）

### 4-1. 【要確認】ログイン成功時にAuthSession.userIdが空文字列になる

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
  - このテストケースは `app/src/test/java/com/golfmatch/app/data/mapper/AuthMapperTest.kt` の
    `ログインレスポンス相当(userなし)ではuserIdが空文字列にフォールバックする` に再現コードとして残してある（Pass/Failで判定するものではなく、現状挙動を明文化する目的）。

### 4-2. 【軽微・参考】BlockDtoが未使用

- 対象コード: `app/src/main/java/com/golfmatch/app/data/dto/BlockDto.kt`
- 技術設計書6-3章のブロック関連API（`POST /users/{id}/block`, `DELETE /users/{id}/block`, `GET /users/me/blocks`）は、`ApiService`上では`GET /users/me/blocks`が`List<UserDto>`を返す実装になっており（`BlockedUsersUiState.blockedUsers: List<User>`と整合させるための妥当な選択）、`BlockDto`はどこからも参照されていない（対応する`BlockMapper`も存在しない）。
- 動作上の不具合ではなくデッドコードの指摘のみ。ブロック関連の管理API（例:ブロック日時を含む一覧等）を将来追加する場合の設計メモとして残すか、不要であれば削除を検討されたい。バグではないため差し戻し必須ではない。

---

## 5. 結論

現時点でテスト可能な範囲（domain/model, domain/usecase, data/mapper）について、技術設計書5章のデータモデル定義との齟齬は見つからなかった。UseCase層はいずれもRepositoryへの薄い委譲であり、レコメンドスコアリング等の主要ビジネスロジックはサーバー側実装待ちのためテスト対象外とした。1件、ログイン成功後の`userId`欠落につながりうる実装上の懸念点（4-1）を発見したため、DeveloperAgent/ArchitectAgentへの確認を推奨する。
