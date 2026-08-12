import { setGlobalOptions } from "firebase-functions/v2";
import { defineSecret } from "firebase-functions/params";
import { onRequest } from "firebase-functions/v2/https";
import { createApp } from "./app";

// Cloud Functions実行リージョン。技術設計書に明記が無いため、日本国内ユーザー向けサービスであることを
// 踏まえDeveloperAgentの実装判断として東京リージョンを既定値に設定した（実デプロイ時に運用判断で変更可）。
setGlobalOptions({ region: "asia-northeast1" });

/**
 * Twilioの認証情報（技術設計書12-5章）。Secret Managerで管理し、実行時に`process.env`へ注入される。
 * v2の関数は`secrets: [...]`で明示的にバインドしないと注入されないため、下記`api`関数に紐付けている。
 *
 * `defineSecret()`自体はモジュールロード時にSecret Managerへアクセスしないため、シークレット未設定の
 * 環境（Emulator・CI・テスト）でもロードは失敗しない。値が無い場合の挙動は`TwilioSmsSender`側で扱う
 * （SMS送信時に設定不備をログ出力し500を返す）。設定手順はfunctions/README.md参照。
 */
const twilioAccountSid = defineSecret("TWILIO_ACCOUNT_SID");
const twilioAuthToken = defineSecret("TWILIO_AUTH_TOKEN");
const twilioFromNumber = defineSecret("TWILIO_FROM_NUMBER");

/**
 * Express製単一HTTPS関数（技術設計書12-1章、ADR-0008）。
 * エンドポイントごとに個別のCloud Functionを立てず、Expressアプリ1つを`api`関数として公開する。
 */
export const api = onRequest({ secrets: [twilioAccountSid, twilioAuthToken, twilioFromNumber] }, createApp());
