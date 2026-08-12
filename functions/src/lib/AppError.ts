/** 技術設計書12-6章で定義されたエラーコード一覧。 */
export type AppErrorCode =
  | "VALIDATION_ERROR"
  | "UNAUTHENTICATED"
  | "FORBIDDEN"
  | "NOT_FOUND"
  | "CONFLICT"
  | "BLOCKED"
  | "RATE_LIMITED"
  | "INTERNAL";

/**
 * 業務エラーを表す共通クラス（技術設計書12-1章・12-6章）。
 * `errorHandler`ミドルウェアが捕捉し、`{ error: { code, message } }`形式に変換する。
 */
export class AppError extends Error {
  readonly httpStatus: number;
  readonly code: AppErrorCode;

  constructor(httpStatus: number, code: AppErrorCode, message: string) {
    super(message);
    this.name = "AppError";
    this.httpStatus = httpStatus;
    this.code = code;
  }
}
