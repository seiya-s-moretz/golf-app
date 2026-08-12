/**
 * 2ユーザーIDの正規化ペアキー生成（技術設計書12-2-1章）。
 * Connection/Message/Blockのドキュメント設計で共用する。
 */
export function buildPairId(userIdA: string, userIdB: string): string {
  return [userIdA, userIdB].sort().join("_");
}

/** `userAId < userBId` となるよう正規化する（技術設計書5-2章 Connection定義）。 */
export function normalizePair(userIdA: string, userIdB: string): { userAId: string; userBId: string } {
  const [userAId, userBId] = [userIdA, userIdB].sort();
  return { userAId, userBId };
}
