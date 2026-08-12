/**
 * Firestore Emulatorのデータを一括クリアするヘルパー（テスト間の独立性確保用）。
 * Firestore Emulatorが提供する管理用REST API（本番Firestoreには存在しない）を使用する。
 * https://firebase.google.com/docs/emulator-suite/connect_firestore#clear_your_database_between_tests
 */
export async function clearFirestoreEmulator(): Promise<void> {
  const host = process.env.FIRESTORE_EMULATOR_HOST;
  if (!host) {
    throw new Error("FIRESTORE_EMULATOR_HOST が設定されていません。");
  }
  const projectId = process.env.GCLOUD_PROJECT ?? "golf-app-dev-placeholder";
  const res = await fetch(
    `http://${host}/emulator/v1/projects/${projectId}/databases/(default)/documents`,
    { method: "DELETE" }
  );
  if (!res.ok) {
    throw new Error(`Firestore Emulatorのデータクリアに失敗しました: ${res.status} ${await res.text()}`);
  }
}
