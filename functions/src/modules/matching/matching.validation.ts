import { z } from "zod";

/** `GET /users/me/match-requests`クエリパラメータ（技術設計書6-5章）。 */
export const listMatchRequestsQuerySchema = z.object({
  direction: z.enum(["received", "sent"], {
    errorMap: () => ({ message: "directionはreceivedまたはsentで指定してください" }),
  }),
});
