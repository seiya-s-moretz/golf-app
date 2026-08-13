# Twilio切替チェックリスト（`SMS_PROVIDER=console` の撤去手順）

作成日: 2026-08-13
対象: 本番環境（Firebaseプロジェクト `seiya-app-818a4`）

本番のOTP送信を、暫定の「ログ出力のみ」から実SMS送信へ切り替えるための手順書。
実装（`TwilioSmsSender`・Secret Managerバインド）は完了済みで、**残っているのはアカウント取得と設定作業のみ**である。

---

## 0. 現状と、なぜ急ぐ必要があるか

`functions/.env.seiya-app-818a4` の `SMS_PROVIDER=console` により、本番のCloud Functionsは
`ConsoleSmsSender`（実送信せずCloud Loggingに出力するだけ）を使っている。

> **この状態では、Cloud Loggingの閲覧権限を持つ者が任意の電話番号のOTPを読み取り、その電話番号の
> ユーザーとして認証できてしまう。** 実ユーザーの受け入れ前に必ず撤去すること。

一方で、この暫定設定のおかげでTwilio契約前でも認証フローの疎通確認ができている（技術設計書12-5章、ADR-0008）。
**「実ユーザーを入れる直前」が撤去のタイミング**であり、それまでは意図的にこのままでよい。

---

## 1. Twilioアカウントの取得（ユーザー作業）

- [ ] Twilioアカウントを作成する
- [ ] **Upgrade（クレジットカード登録）を行う**
  - トライアルアカウントは「検証済み番号」にしか送信できず、番号購入時にも自分の番号のVerified Caller ID登録が必要。自分の端末1台での確認だけならトライアルでも可能だが、実ユーザーを入れるならUpgrade必須
- [ ] 送信元番号を購入する（**米国のLocal番号**）
  - **Twilioで購入した日本の電話番号はSMSの送信元に使えない**（日本番号を送信元にしたい場合はTwilioのオプション「国内SMS」の契約が別途必要）。通常は米国番号を送信元にする
  - 目安コスト: 番号維持 **約 $1.15/月**、日本の携帯宛て送信 **約 $0.089/通**（2026-08時点。1通が複数セグメントに分割されると通数分課金される）
- [ ] 購入した番号を **E.164形式**（`+1XXXXXXXXXX`）で控える

> ⚠️ 上記の料金・制約は変わりやすい。**着手時に必ず最新のTwilio公式情報で再確認すること。**
> 日本では英数字送信者ID（Alphanumeric Sender ID）も選択肢になるが、**有料アカウントのみ**で、
> 国によっては事前登録（審査）が必要。到達率を優先するなら国内SMSオプションも比較検討する。

---

## 2. 認証情報をSecret Managerに登録（ユーザー作業）

**認証情報はチャット等に貼らず、必ずユーザー自身のターミナルで実行する**（実行後に値の入力を求められる）。

```bash
cd functions
npx firebase functions:secrets:set TWILIO_ACCOUNT_SID
npx firebase functions:secrets:set TWILIO_AUTH_TOKEN
npx firebase functions:secrets:set TWILIO_FROM_NUMBER   # E.164形式（例: +15551234567）
```

- [ ] 3つとも登録した
- [ ] 登録内容を確認した（現在は仮値 `PLACEHOLDER_NOT_CONFIGURED` が入っている）

```bash
npx firebase functions:secrets:access TWILIO_ACCOUNT_SID   # 個別確認
npx firebase functions:secrets:get                         # 一覧
```

---

## 3. 暫定設定の撤去とデプロイ

- [ ] `functions/.env.seiya-app-818a4` から **`SMS_PROVIDER=console` の行を削除する**
  - ファイル末尾の該当行のみを消す。上部の注意書きコメントも不要なら合わせて整理してよい
  - 削除するだけでよい（`SMS_PROVIDER` 未設定かつEmulator以外では `TwilioSmsSender` が既定で選ばれる。`functions/src/modules/auth/sms/index.ts`）
- [ ] デプロイする

```bash
cd functions
npm run deploy        # Functionsのみ
# Firestoreルール・インデックスが未デプロイなら（README「デプロイ」参照）
npm run deploy:all
```

> `firebase deploy` は、バインド対象のシークレットが存在しないとエラーになる。手順2を先に済ませること。

---

## 4. 切替後の動作確認

- [ ] **自分の携帯にOTPが実際に届く**

```bash
curl -X POST https://asia-northeast1-seiya-app-818a4.cloudfunctions.net/api/auth/phone/otp \
  -H "Content-Type: application/json" \
  -d '{"phone_number":"+819012345678"}'
```

- [ ] **Cloud LoggingにOTPコードが出ていない**（これが本作業の主目的）

```bash
cd functions
npx firebase functions:log
```

`TwilioSmsSender` はSMS本文・認証情報を一切ログ出力せず、宛先番号も下4桁のみに伏せる。
**ログ中にOTPの6桁が見えたら切替が効いていない**（`SMS_PROVIDER=console` の削除漏れ、または再デプロイ漏れ）。

- [ ] Androidアプリから電話番号入力 → OTP認証 → プロフィール登録まで通す
  - クライアントは国内表記（`09012345678`）をE.164へ正規化して送る（`PhoneNumberNormalizer`）。Twilioも宛先はE.164形式である必要があるため整合している

---

## 5. うまくいかないときの切り分け

| 症状 | 確認すること |
|---|---|
| SMSが届かない・500が返る | Cloud Loggingを確認する。`TwilioSmsSender` は認証情報が未設定の場合、**どの環境変数が欠けているかだけ**をログに出し、クライアントには `500 / INTERNAL` を返す |
| トライアルのまま送信して失敗する | 宛先が「検証済み番号」に限定されていないか。Upgrade済みか |
| 送信元番号が拒否される | `TWILIO_FROM_NUMBER` がE.164形式か。日本番号を送信元にしていないか（前述のとおり使用不可） |
| 設定を変えたのに反映されない | シークレット更新後は**再デプロイが必要**（新しいバージョンが関数にバインドされる） |

### 緊急時の戻し方

`functions/.env.seiya-app-818a4` に `SMS_PROVIDER=console` を書き戻して再デプロイすれば元の暫定状態に戻せる。
ただし **0章のセキュリティリスクが再び発生する**ため、実ユーザーがいる状態では行わないこと。

---

## 6. 完了後にやること

- [ ] `functions/.env.seiya-app-818a4` に `SMS_PROVIDER=console` が残っていないことを最終確認する
- [ ] 本チェックリストの完了を `docs/test-plan.md` または開発メモに記録する
- [ ] コスト監視: Twilioコンソールで残高アラートを設定しておくと、想定外の送信増に気づける

---

## 参考

- 実装・設定の詳細: `functions/README.md`「Twilio（実SMS送信）の設定」
- 設計上の位置づけ: `docs/技術設計書.md` 12-5章、`docs/adr/0008-server-implementation-design.md`
- SMS送信実装: `functions/src/modules/auth/sms/`（`SmsSender` インターフェース＋`ConsoleSmsSender` / `TwilioSmsSender`）
