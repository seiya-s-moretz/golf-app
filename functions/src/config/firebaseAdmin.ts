import { getApps, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

if (getApps().length === 0) {
  initializeApp();
}

/** Firestore Admin SDKインスタンス（シングルトン、技術設計書12-1章）。 */
export const db = getFirestore();
