import type { NextFunction, Request, Response } from "express";
import { AppError } from "../lib/AppError";

/**
 * 管理者権限ミドルウェア（技術設計書12-4章、ADR-0007）。
 * `authenticate`の後段に適用し、同じ`User`の`isAdmin`フラグのみを検証する薄い実装。
 * 別建ての管理者認証基盤は持たない。Phase3の`/admin/*`配下で使用する（Phase1時点では未使用）。
 */
export function requireAdmin(req: Request, _res: Response, next: NextFunction): void {
  if (!req.currentUser?.isAdmin) {
    next(new AppError(403, "FORBIDDEN", "管理者権限が必要です"));
    return;
  }
  next();
}
