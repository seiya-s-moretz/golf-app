import { setGlobalOptions } from "firebase-functions/v2";
import { onRequest } from "firebase-functions/v2/https";
import { createApp } from "./app";

// Cloud Functions実行リージョン。技術設計書に明記が無いため、日本国内ユーザー向けサービスであることを
// 踏まえDeveloperAgentの実装判断として東京リージョンを既定値に設定した（実デプロイ時に運用判断で変更可）。
setGlobalOptions({ region: "asia-northeast1" });

/**
 * Express製単一HTTPS関数（技術設計書12-1章、ADR-0008）。
 * エンドポイントごとに個別のCloud Functionを立てず、Expressアプリ1つを`api`関数として公開する。
 */
export const api = onRequest(createApp());
