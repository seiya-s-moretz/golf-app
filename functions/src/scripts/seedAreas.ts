/**
 * エリアマスタ（AreaMaster）の開発用シードデータ投入スクリプト（技術設計書12-8章）。
 *
 * `GET /areas`・`POST /users`（`area_id`バリデーション）の動作確認に必要な最低限のダミーデータを投入する。
 * エリア名自体は未確定（技術設計書10章#5）であり、実データではなく開発用シードとして扱う。
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
 */
import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";

if (!process.env.FIRESTORE_EMULATOR_HOST) {
  // eslint-disable-next-line no-console
  console.error(
    "FIRESTORE_EMULATOR_HOST が設定されていません。本番Firestoreへの誤投入を避けるため処理を中止します。\n" +
      "例: FIRESTORE_EMULATOR_HOST=localhost:8080 npm run seed:areas"
  );
  process.exit(1);
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

const SEED_AREAS: SeedArea[] = [
  {
    areaId: "00000000-0000-4000-8000-000000000001",
    prefecture: "東京都",
    areaName: "東京23区",
    displayOrder: 1,
    isActive: true,
  },
  {
    areaId: "00000000-0000-4000-8000-000000000002",
    prefecture: "東京都",
    areaName: "多摩地域",
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
  {
    areaId: "00000000-0000-4000-8000-000000000004",
    prefecture: "埼玉県",
    areaName: "さいたま市",
    displayOrder: 4,
    isActive: true,
  },
  {
    // is_active=falseのサンプル（将来のエリア拡大用の先行登録例。技術設計書5-2章の設計意図の確認用）
    areaId: "00000000-0000-4000-8000-000000000005",
    prefecture: "千葉県",
    areaName: "千葉市",
    displayOrder: 5,
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
