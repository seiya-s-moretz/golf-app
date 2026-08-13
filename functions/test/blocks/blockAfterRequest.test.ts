import request from "supertest";
import { createApp } from "../../src/app";
import { db } from "../../src/config/firebaseAdmin";
import { buildPairId } from "../../src/lib/pairId";
import { authHeader, registerNewUser } from "../helpers/fixtures";

/**
 * 「申請したあとにブロックした」場合のブロック適用（技術設計書5-2章）。
 *
 * ブロック判定は申請作成時にしか行われていなかったため、申請後にブロックしても承認でき、
 * ブロック関係のままConnectionが作られていた。Connectionができると会話一覧には出るのに
 * メッセージ送信は403で拒否されるため、**消せない死んだ会話**が双方に残る。
 * 一覧側も同様に、ブロック後も相手の申請が見え続け承認できてしまっていた。
 */
describe("申請後にブロックした場合の扱い", () => {
  const app = createApp();

  describe("マッチング申請", () => {
    test("申請後にブロックすると承認は403 BLOCKEDになりConnectionも作られない", async () => {
      const from = await registerNewUser(app);
      const to = await registerNewUser(app);
      const created = await request(app)
        .post(`/users/${to.userId}/match-requests`)
        .set(...authHeader(from.accessToken))
        .expect(201);

      // 申請を受け取った側が申請者をブロックする
      await request(app)
        .post(`/users/${from.userId}/block`)
        .set(...authHeader(to.accessToken))
        .expect(204);

      const res = await request(app)
        .post(`/match-requests/${created.body.match_request_id}/approve`)
        .set(...authHeader(to.accessToken))
        .expect(403);
      expect(res.body.error.code).toBe("BLOCKED");

      const connectionSnap = await db.collection("connections").doc(buildPairId(from.userId, to.userId)).get();
      expect(connectionSnap.exists).toBe(false);
    });

    test("ブロックした相手からの申請は受信一覧から除外される", async () => {
      const from = await registerNewUser(app);
      const to = await registerNewUser(app);
      const created = await request(app)
        .post(`/users/${to.userId}/match-requests`)
        .set(...authHeader(from.accessToken))
        .expect(201);

      await request(app)
        .post(`/users/${from.userId}/block`)
        .set(...authHeader(to.accessToken))
        .expect(204);

      const res = await request(app)
        .get("/users/me/match-requests?direction=received")
        .set(...authHeader(to.accessToken))
        .expect(200);
      expect(
        res.body.find((r: { match_request_id: string }) => r.match_request_id === created.body.match_request_id)
      ).toBeUndefined();
    });
  });

  test("停止中(SUSPENDED)のユーザーはおすすめに出ない（申請しても承認されようがないため）", async () => {
    const me = await registerNewUser(app);
    // 同一エリア・同一スコア・同一目的でスコア100点になる相手を作る
    const other = await registerNewUser(app, { areaId: me.areaId, averageScore: 100, purpose: "CASUAL" });

    const before = await request(app)
      .get("/users/recommend")
      .set(...authHeader(me.accessToken))
      .expect(200);
    expect(before.body.find((u: { user_id: string }) => u.user_id === other.userId)).toBeDefined();

    // 運用による利用停止を想定（status変更APIはMVPに無いためDBを直接更新する）
    await db.collection("users").doc(other.userId).update({ status: "SUSPENDED" });

    const after = await request(app)
      .get("/users/recommend")
      .set(...authHeader(me.accessToken))
      .expect(200);
    expect(after.body.find((u: { user_id: string }) => u.user_id === other.userId)).toBeUndefined();
  });

  describe("ラウンド参加申請", () => {
    async function createEventAndApply() {
      const organizer = await registerNewUser(app);
      const applicant = await registerNewUser(app);
      const event = await request(app)
        .post("/round-events")
        .set(...authHeader(organizer.accessToken))
        .send({ club_name: "テストGC", datetime: "2026-09-01T09:00:00+09:00", fee: 5000, capacity: 4 })
        .expect(201);
      const joinRes = await request(app)
        .post(`/round-events/${event.body.event_id}/join-requests`)
        .set(...authHeader(applicant.accessToken))
        .expect(201);
      return { organizer, applicant, eventId: event.body.event_id, joinRequestId: joinRes.body.join_request_id };
    }

    test("申請後にブロックすると承認は403 BLOCKEDになりcurrentも加算されない", async () => {
      const { organizer, applicant, eventId, joinRequestId } = await createEventAndApply();

      await request(app)
        .post(`/users/${applicant.userId}/block`)
        .set(...authHeader(organizer.accessToken))
        .expect(204);

      const res = await request(app)
        .post(`/round-events/${eventId}/join-requests/${joinRequestId}/approve`)
        .set(...authHeader(organizer.accessToken))
        .expect(403);
      expect(res.body.error.code).toBe("BLOCKED");

      const eventRes = await request(app)
        .get(`/round-events/${eventId}`)
        .set(...authHeader(organizer.accessToken))
        .expect(200);
      expect(eventRes.body.current).toBe(0);
    });

    test("ブロックした相手の募集は単体取得でも404になる（一覧と挙動を揃える）", async () => {
      const { organizer, applicant, eventId } = await createEventAndApply();

      await request(app)
        .post(`/users/${organizer.userId}/block`)
        .set(...authHeader(applicant.accessToken))
        .expect(204);

      const res = await request(app)
        .get(`/round-events/${eventId}`)
        .set(...authHeader(applicant.accessToken))
        .expect(404);
      expect(res.body.error.code).toBe("NOT_FOUND");

      // 主催者自身は当然取得できる
      await request(app)
        .get(`/round-events/${eventId}`)
        .set(...authHeader(organizer.accessToken))
        .expect(200);
    });

    test("ブロックした相手の参加申請は一覧から除外される", async () => {
      const { organizer, applicant, eventId, joinRequestId } = await createEventAndApply();

      await request(app)
        .post(`/users/${applicant.userId}/block`)
        .set(...authHeader(organizer.accessToken))
        .expect(204);

      const res = await request(app)
        .get(`/round-events/${eventId}/join-requests`)
        .set(...authHeader(organizer.accessToken))
        .expect(200);
      expect(
        res.body.find((r: { join_request_id: string }) => r.join_request_id === joinRequestId)
      ).toBeUndefined();
    });
  });
});
