# ゴルフマッチングアプリ（golf-app）

特定エリア内でレベル・目的の合うゴルフ仲間を見つけられる、Android向けコミュニティアプリ。
「スコア差・エリア・目的の一致度」によるレコメンドを軸に、ラウンド募集・おすすめユーザー・掲示板・マイページの4つのコア機能に加え、
安全に利用するための簡易メッセージ・通報/ブロック・簡易本人確認（SMS OTP）を備える。

詳細な要件・設計は以下を参照。

- 要件定義: `docs/要件定義書.md`
- 技術設計: `docs/技術設計書.md`
- 設計判断（ADR）: `docs/adr/0001-round-join-approval-flow.md` ほか

## 現在の状態

Androidクライアント側は4コア画面＋認証フロー画面のUI〜データ層実装が完了している。
サーバーサイド（`functions/`, Cloud Functions for Firebase）はPhase1（認証基盤・エリアマスタ・
ユーザープロフィール・ラウンド募集）を実装済み。Phase2（おすすめユーザー・マッチング申請・掲示板）、
Phase3（メッセージ・通報・ブロック・通報管理）は未実装（技術設計書13章）。セットアップ・Emulatorでの
動作確認方法は `functions/README.md` を参照。

## 技術スタック

- 言語: Kotlin
- UI: Jetpack Compose（Material3） + Navigation Compose
- アーキテクチャ: MVVM + Container + Clean Architecture（`ui` / `domain` / `data` / `di`）
- DI: Hilt
- バックエンド: Firebase（Authentication / Firestore / Cloud Functions）
- 通信: Retrofit + OkHttp + Gson（Cloud FunctionsのHTTPSエンドポイント呼び出し用）
- 非同期: Kotlin Coroutines
- 日時: kotlinx-datetime

バージョン管理は Gradle Version Catalog（`gradle/libs.versions.toml`）で行う。

## ディレクトリ構成

```
app/src/main/java/com/golfmatch/app/
 ├ GolfMatchApplication.kt   … Hiltエントリポイント
 ├ MainActivity.kt           … Compose起点、NavGraphをホスト
 ├ domain/
 │  ├ model/                 … ビジネスモデル（User, RoundEvent, Message 等）
 │  ├ repository/            … リポジトリインターフェース
 │  └ usecase/                … ユースケース（@Inject constructor + operator fun invoke）
 ├ data/
 │  ├ api/ApiService.kt      … Retrofitインターフェース（Cloud Functions呼び出し）
 │  ├ dto/                   … APIリクエスト/レスポンスDTO
 │  ├ mapper/                … DTO⇔Domainモデル変換
 │  ├ repository/impl/       … リポジトリ実装（ApiServiceを呼び出す薄い実装。本格的な接続処理は次フェーズ）
 │  └ auth/AuthSessionManager.kt … アクセストークンの保持（Bearer認証、ADR-0003）
 ├ di/                       … Hiltモジュール（NetworkModule / RepositoryModule / UseCaseModule）
 └ ui/
    ├ navigation/            … Route定義・NavGraph（画面本体は次フェーズで実装）
    └ theme/                 … Compose Material3テーマ（暫定カラー）
```

画面ごとの `ui/screen` `ui/container` `ui/viewmodel` `ui/component` は次フェーズ（画面実装）で追加する
（技術設計書 4章のディレクトリ構成に準拠予定）。

## セットアップ手順

### 前提
- Android Studio（最新安定版を推奨）
- JDK 17
- Android SDK（`compileSdk 34` / `minSdk 26`）

### 1. リポジトリ取得
```
git clone https://github.com/seiya-s-moretz/golf-app.git
```

### 2. google-services.json の配置
本アプリはFirebase（Auth / Firestore / Functions）を使用する。Firebase Consoleで作成済みのプロジェクトから、
Androidアプリ（パッケージ名 `com.golfmatch.app`）を登録して `google-services.json` をダウンロードし、
以下のパスに配置する。

```
app/google-services.json
```

このファイルはプロジェクト固有の設定を含むため `.gitignore` でコミット対象外としている。
配置しない場合、`app/google-services.json` にプレースホルダー（ダミー値）が置かれた状態でも
Gradle同期・ビルド自体は通るが、実際のFirebase接続は行えない。

### 3. local.properties の設定
Android SDKのパスを指定する（Android Studioでプロジェクトを開けば自動生成される）。
```
sdk.dir=<Android SDKのパス>
```
このファイルもコミット対象外。

### 4. ビルド
```
./gradlew :app:assembleDebug
```

## 主要な実装判断メモ（雛形構築フェーズ）

- **ID型**: Firestoreのドキュメント運用を踏まえ、技術設計書のUUID型フィールドはすべて `String` として実装した。
- **日時型**: `domain`層はフレームワーク非依存を保つため `kotlinx.datetime.Instant` を採用（`java.time`はAndroidのdesugaring設定に依存するため回避）。
- **API通信方式**: `ApiService`（Retrofit）はCloud FunctionsのHTTPSエンドポイントを技術設計書6章のパスに沿って呼び出す設計とした。
  Firebase Auth（電話番号認証）やFirestoreクライアントSDKの直接利用に切り替える可能性はあるが、
  現時点では技術設計書のAPI仕様（エンドポイント一覧）をそのまま実装するのが最も設計書に忠実と判断した。
  `NetworkModule`では`FirebaseAuth`/`FirebaseFirestore`/`FirebaseFunctions`のインスタンスも提供しており、
  次フェーズでの使い分けの決定に対応できる状態にしてある。
- **UseCaseModule**: 参考資料（`D:\勉強\golf\設計書.md`）はUseCaseごとに明示的な`@Provides`を記載しているが、
  本プロジェクトでは各UseCaseクラスに`@Inject constructor`を付与し、Hiltによる自動解決に統一した
  （明示的Provideと`@Inject constructor`を併用すると型の重複束縛エラーになるため）。`UseCaseModule.kt`は
  ディレクトリ構成上の対応点として空モジュールで残している。
- **認証トークンの保持**: `AuthSessionManager`（プロセス内メモリ保持のみ）を新設し、`NetworkModule`のOkHttp Interceptorが
  Bearerトークンを付与する。アプリ再起動をまたぐ永続化（DataStore等）は次フェーズで検討する。
- **Cloud FunctionsのベースURL**: `NetworkModule`内にプレースホルダーURLを設定している。実際のデプロイ先が
  確定次第、差し替えが必要（要 ArchitectAgent/DeveloperAgent確認）。

## 次フェーズで必要な作業

- 各画面のCompose UI実装（`ui/screen` `ui/container` `ui/viewmodel` `ui/component`、技術設計書 3章・7章）
- Repositoryの実接続処理（Cloud Functions呼び出しの実データ確認、エラーハンドリング）
- Firebase Cloud Functions側の実装（技術設計書 6章のAPI仕様に基づくサーバーサイド実装）
- `google-services.json` の実データ配置
- 技術設計書10章の要確認事項（特に#3 参加承認フローへの変更、#6 サーバーサイド技術選定の最終確認）
