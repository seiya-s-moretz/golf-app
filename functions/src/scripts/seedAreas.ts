/**
 * エリアマスタ（AreaMaster）のシードデータ投入スクリプト（技術設計書12-8章）。
 *
 * `GET /areas`・`POST /users`（`area_id`バリデーション）に必要なエリアマスタを投入する。
 * 2026-08-13にプロダクトオーナーが初期展開エリアを確定させたため（技術設計書10章#5）、
 * 本スクリプトの内容は開発用ダミーではなく**本番投入する実データ**である。
 *
 * 使用方法:
 *   1. `firebase emulators:start --only functions,firestore,auth` でEmulatorを起動する
 *   2. 別ターミナルで以下を実行する（Firestoreエミュレータのポートは`firebase.json`の設定=8080に合わせる）
 *
 *        cd functions
 *        FIRESTORE_EMULATOR_HOST=localhost:8080 npm run seed:areas
 *
 * 冪等: 固定UUID文字列をドキュメントIDとして使うため、複数回実行しても重複作成されず上書きされる。
 * 安全策として`FIRESTORE_EMULATOR_HOST`が未設定の場合は実行を中止する（本番Firestoreへの誤投入防止）。
 *
 * 本番Firestoreへ意図的に投入する場合のみ、明示的なオプトインとして`SEED_TARGET=production`と
 * 対象プロジェクトの`GCLOUD_PROJECT`を指定する（暗黙に本番へ向かないよう、両方の指定を必須とする）。
 * 認証はAdmin SDKのApplication Default Credentialsに従う（`GOOGLE_APPLICATION_CREDENTIALS`で
 * サービスアカウント鍵を指定するか、`gcloud auth application-default login`を実行しておく）。
 *
 *      GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json \
 *      SEED_TARGET=production GCLOUD_PROJECT=<プロジェクトID> npm run seed:areas
 *
 * 投入するエリアは初期展開エリア（`isActive=true`）と、拡大候補の先行登録（`isActive=false`）から成る。
 * エリア拡大時は`isActive`をtrueに変更するだけでよく、アプリ本体の改修は不要（ADR-0002）。
 */
import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";

const isProductionSeed = process.env.SEED_TARGET === "production";

if (!isProductionSeed && !process.env.FIRESTORE_EMULATOR_HOST) {
  // eslint-disable-next-line no-console
  console.error(
    "FIRESTORE_EMULATOR_HOST が設定されていません。本番Firestoreへの誤投入を避けるため処理を中止します。\n" +
      "例: FIRESTORE_EMULATOR_HOST=localhost:8080 npm run seed:areas\n" +
      "本番へ意図的に投入する場合は SEED_TARGET=production GCLOUD_PROJECT=<プロジェクトID> を指定してください。"
  );
  process.exit(1);
}

if (isProductionSeed) {
  if (process.env.FIRESTORE_EMULATOR_HOST) {
    // eslint-disable-next-line no-console
    console.error(
      "SEED_TARGET=production と FIRESTORE_EMULATOR_HOST が同時に指定されています。" +
        "投入先が曖昧なため処理を中止します。"
    );
    process.exit(1);
  }
  if (!process.env.GCLOUD_PROJECT) {
    // eslint-disable-next-line no-console
    console.error("SEED_TARGET=production の場合は GCLOUD_PROJECT に対象プロジェクトIDを指定してください。");
    process.exit(1);
  }
  // eslint-disable-next-line no-console
  console.warn(`【注意】本番Firestore（project=${process.env.GCLOUD_PROJECT}）へ投入します。`);
}

initializeApp({ projectId: process.env.GCLOUD_PROJECT ?? "golf-app-dev-placeholder" });
const db = getFirestore();

interface SeedArea {
  areaId: string;
  prefecture: string;
  areaName: string;
  displayOrder: number;
  isActive: boolean;
}

/**
 * 初期展開エリア（2026-08-13確定、技術設計書10章#5）。
 *
 * 「東京23区＋川崎市＋横浜市」の3エリアのみを`isActive=true`とする。20-30代ゴルファーの母数が
 * 最も厚い範囲に絞ってコミュニティ密度を優先する方針（要件定義書2章の決定事項#2）。
 * 東京23区は区・ブロック単位に分割せず1レコードとする（ローンチ初期の母数ではエリアを細分化すると
 * おすすめユーザーの同エリア加点がほぼ機能しなくなるため）。
 *
 * 多摩地域・さいたま市・千葉市はエリア拡大の候補として`isActive=false`で先行登録しておく。
 * `areaId`は既存の投入済みデータと一致させ、再実行時に重複作成せず上書きされるようにしている。
 */
const SEED_AREAS: SeedArea[] = [
  {
    areaId: "00000000-0000-4000-8000-000000000001",
    prefecture: "東京都",
    areaName: "東京23区",
    displayOrder: 1,
    isActive: true,
  },
  {
    areaId: "00000000-0000-4000-8000-000000000006",
    prefecture: "神奈川県",
    areaName: "川崎市",
    displayOrder: 2,
    isActive: true,
  },
  {
    areaId: "00000000-0000-4000-8000-000000000003",
    prefecture: "神奈川県",
    areaName: "横浜市",
    displayOrder: 3,
    isActive: true,
  },
  // 以下はエリア拡大候補の先行登録（選択肢には出さない。ADR-0002）
  {
    areaId: "00000000-0000-4000-8000-000000000002",
    prefecture: "東京都",
    areaName: "多摩地域",
    displayOrder: 4,
    isActive: false,
  },
  {
    areaId: "00000000-0000-4000-8000-000000000004",
    prefecture: "埼玉県",
    areaName: "さいたま市",
    displayOrder: 5,
    isActive: false,
  },
  {
    areaId: "00000000-0000-4000-8000-000000000005",
    prefecture: "千葉県",
    areaName: "千葉市",
    displayOrder: 6,
    isActive: false,
  },
];

async function main(): Promise<void> {
  const now = Timestamp.now();
  const batch = db.batch();
  for (const area of SEED_AREAS) {
    const ref = db.collection("areaMasters").doc(area.areaId);
    batch.set(ref, {
      areaId: area.areaId,
      prefecture: area.prefecture,
      areaName: area.areaName,
      displayOrder: area.displayOrder,
      isActive: area.isActive,
      createdAt: now,
    });
  }
  await batch.commit();
  // eslint-disable-next-line no-console
  console.log(`areaMasters: ${SEED_AREAS.length}件のシードデータを投入しました。`);
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    // eslint-disable-next-line no-console
    console.error(err);
    process.exit(1);
  });
