/**
 * Firestoreセキュリティルールの検証（技術設計書12-7章、`firestore.rules`）。
 *
 * 本プロジェクトはAndroidクライアントから業務データへ直接アクセスさせず、常にCloud Functions
 * （Admin SDK＝ルールをバイパスする信頼された実行コンテキスト）経由でアクセスする設計であり、
 * `firestore.rules`は全コレクション一律拒否になっている。
 *
 * この「一律拒否」が本当に効いているかは、Admin SDKを使う他の統合テストでは検証できない
 * （Admin SDKはルールを迂回するため）。そこで本ファイルのみ`@firebase/rules-unit-testing`で
 * クライアントSDK相当のコンテキストを作り、未認証・認証済みのいずれからも読み書きが
 * 拒否されることを確認する。ルールを緩めた場合はここが落ちる。
 */
import { readFileSync } from "fs";
import { resolve } from "path";
import {
  assertFails,
  initializeTestEnvironment,
  type RulesTestEnvironment,
} from "@firebase/rules-unit-testing";

/** 業務データを保持する主要コレクション（技術設計書5章のデータモデル） */
const COLLECTIONS = [
  "users",
  "areaMasters",
  "roundEvents",
  "boardPosts",
  "matchRequests",
  "connections",
  "messages",
  "blocks",
  "reports",
  "sessions",
  "phoneVerifications",
];

let testEnv: RulesTestEnvironment;

beforeAll(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST ?? "localhost:8080").split(":");
  testEnv = await initializeTestEnvironment({
    // firebase.jsonで`singleProjectMode: true`にしているため、Emulator起動時のプロジェクトIDと
    // 揃える必要がある。本テストは拒否されることの確認しか行わず、データを残さないため衝突しない。
    projectId: process.env.GCLOUD_PROJECT ?? "golf-app-dev-placeholder",
    firestore: {
      rules: readFileSync(resolve(__dirname, "../../../firestore.rules"), "utf8"),
      host,
      port: Number(port),
    },
  });
});

afterAll(async () => {
  await testEnv?.cleanup();
});

describe("firestore.rules（クライアントからの直接アクセス）", () => {
  test.each(COLLECTIONS)("未認証クライアントは %s を読み書きできない", async (collection) => {
    const db = testEnv.unauthenticatedContext().firestore();
    await assertFails(db.collection(collection).doc("doc-1").get());
    await assertFails(db.collection(collection).doc("doc-1").set({ value: 1 }));
  });

  test.each(COLLECTIONS)("認証済みクライアントでも %s を読み書きできない", async (collection) => {
    // Firebase Authでサインイン済みのクライアントを想定（本アプリの認証は独自Bearerトークンだが、
    // 万一Firebase Authを併用しても直接アクセスは許可されないことを確認する）
    const db = testEnv.authenticatedContext("user-1").firestore();
    await assertFails(db.collection(collection).doc("doc-1").get());
    await assertFails(db.collection(collection).doc("doc-1").set({ value: 1 }));
  });

  test("一覧クエリ（collection全体の読み取り）も拒否される", async () => {
    const db = testEnv.authenticatedContext("user-1").firestore();
    await assertFails(db.collection("users").get());
  });

  test("サブコレクション（roundEvents/{id}/joinRequests）も拒否される", async () => {
    const db = testEnv.authenticatedContext("user-1").firestore();
    await assertFails(db.collection("roundEvents").doc("event-1").collection("joinRequests").get());
  });
});
