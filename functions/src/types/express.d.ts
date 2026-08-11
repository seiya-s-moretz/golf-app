import type { UserDoc, UserStatus } from "./firestore";

export interface CurrentUser {
  userId: string;
  isAdmin: boolean;
  status: UserStatus;
  doc: UserDoc;
}

declare global {
  namespace Express {
    interface Request {
      /** `authenticate`ミドルウェアが解決した認証済みユーザー（技術設計書12-4章）。 */
      currentUser?: CurrentUser;
    }
  }
}

export {};
