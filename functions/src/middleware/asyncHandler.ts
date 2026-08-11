import type { NextFunction, Request, RequestHandler, Response } from "express";

/** 非同期routeハンドラのtry/catch省略用ラッパー（技術設計書12-1章・12-6章）。 */
export function asyncHandler(
  handler: (req: Request, res: Response, next: NextFunction) => Promise<unknown>
): RequestHandler {
  return (req, res, next) => {
    handler(req, res, next).catch(next);
  };
}
