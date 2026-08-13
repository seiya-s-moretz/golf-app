import { AppError } from "./AppError";

/**
 * ユーザー入力由来のIDをFirestoreの`doc()`へ渡す前に検証する。
 *
 * `db.collection("users").doc("a/b")`はFirestore SDKが`INVALID_ARGUMENT`を投げる
 * （ドキュメントパスは偶数個の要素である必要がある）。この例外は`AppError`でも`ZodError`でもないため
 * `errorHandler`が500 INTERNALとして扱ってしまい、**入力ミスがサーバー障害として記録される**。
 * URLパスに`%2F`を入れるだけで到達できるため、入口で400として弾く。
 *
 * 空文字・`.`・`..`・`__`前後の予約語もFirestoreのドキュメントID制約に反する。
 */
const INVALID_ID_PATTERN = /[/]/;
const RESERVED_IDS = new Set([".", ".."]);
const MAX_ID_LENGTH = 1500; // FirestoreのドキュメントIDはUTF-8で1500バイトまで

export function assertValidDocumentId(id: string, label = "ID"): void {
  if (
    id.length === 0 ||
    id.length > MAX_ID_LENGTH ||
    INVALID_ID_PATTERN.test(id) ||
    RESERVED_IDS.has(id) ||
    (id.startsWith("__") && id.endsWith("__"))
  ) {
    throw new AppError(400, "VALIDATION_ERROR", `${label}の形式が正しくありません`);
  }
}
