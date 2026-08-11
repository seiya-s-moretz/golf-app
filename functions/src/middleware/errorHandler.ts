import type { NextFunction, Request, Response } from "express";
import * as logger from "firebase-functions/logger";
import { ZodError } from "zod";
import { AppError } from "../lib/AppError";

/**
 * 統一エラーレスポンス整形ミドルウェア（技術設計書12-6章）。
 * `{ error: { code, message } }`形式に統一する。`AppError`以外の未捕捉例外は500・`code=INTERNAL`とし、
 * 詳細はCloud Loggingにのみ出力してレスポンスには含めない（内部実装詳細の漏洩防止）。
 *
 * Expressにエラーハンドリングミドルウェアとして認識させるため引数は4つ必要（`_next`は未使用）。
 */
export function errorHandler(err: unknown, _req: Request, res: Response, _next: NextFunction): void {
  if (err instanceof AppError) {
    res.status(err.httpStatus).json({ error: { code: err.code, message: err.message } });
    return;
  }

  if (err instanceof ZodError) {
    const message = err.issues.map((issue) => `${issue.path.join(".")}: ${issue.message}`).join(" / ");
    res.status(400).json({ error: { code: "VALIDATION_ERROR", message } });
    return;
  }

  logger.error("Unhandled error", err);
  res.status(500).json({ error: { code: "INTERNAL", message: "サーバー内部でエラーが発生しました" } });
}
