import request from "supertest";
import { createApp } from "../../src/app";
import { db } from "../../src/config/firebaseAdmin";
import { buildPairId } from "../../src/lib/pairId";
import { authHeader, registerNewUser } from "../helpers/fixtures";

/**
 * `POST /round-events/{id}/join-requests`とその承認/却下フロー（技術設計書6-4章、ADR-0001）。
 * `capacity > current`検証、二重承認防止、`Connection`作成、`created_by`本人以外は
 * 一覧取得・承認/却下不可であることを検証する。
 */
describe("ラウンド募集・参加申請フロー", () => {
  const app = createApp();

  async function createRoundEvent(accessToken: string, overrides: Record<string, unknown> = {}) {
    const res = await request(app)
      .post("/round-events")
      .set(...authHeader(accessToken))
      .send({
        club_name: "テストゴルフ倶楽部",
        datetime: "2026-09-01T09:00:00+09:00",
        fee: 10000,
        capacity: 2,
        ...overrides,
      })
      .expect(201);
    return res.body;
  }

  test("round-events配下は未認証だと401 UNAUTHENTICATEDを返す", async () => {
    const res = await request(app).get("/round-events").expect(401);
    expect(res.body.error.code).toBe("UNAUTHENTICATED");
  });

  test("POST /round-eventsで募集を作成するとcurrent=0で作成される", async () => {
    const organizer = await registerNewUser(app);
    const event = await createRoundEvent(organizer.accessToken);
    expect(event.current).toBe(0);
    expect(event.created_by).toBe(organizer.userId);
  });

  test("GET /round-events/{id}で単体取得できる（技術設計書6-4章に明記無いがAndroidクライアントが使用するため実装、実装メモ参照）", async () => {
    const organizer = await registerNewUser(app);
    const event = await createRoundEvent(organizer.accessToken);
    const res = await request(app)
      .get(`/round-events/${event.event_id}`)
      .set(...authHeader(organizer.accessToken))
      .expect(200);
    expect(res.body.event_id).toBe(event.event_id);
  });

  test("GET /round-events/{id}は存在しないIDで404 NOT_FOUNDを返す", async () => {
    const organizer = await registerNewUser(app);
    const res = await request(app)
      .get("/round-events/存在しないID")
      .set(...authHeader(organizer.accessToken))
      .expect(404);
    expect(res.body.error.code).toBe("NOT_FOUND");
  });

  test("POST /round-events/{id}/join-requestsで参加申請を作成するとPENDINGで作成され、current加算はされない", async () => {
    const organizer = await registerNewUser(app);
    const applicant = await registerNewUser(app);
    const event = await createRoundEvent(organizer.accessToken);

    const res = await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(applicant.accessToken))
      .expect(201);
    expect(res.body.status).toBe("PENDING");
    expect(res.body.user_id).toBe(applicant.userId);

    const eventRes = await request(app)
      .get(`/round-events/${event.event_id}`)
      .set(...authHeader(organizer.accessToken))
      .expect(200);
    expect(eventRes.body.current).toBe(0);
  });

  test("同一ユーザーが同じ募集へ重複してPENDING申請すると409 CONFLICTを返す", async () => {
    const organizer = await registerNewUser(app);
    const applicant = await registerNewUser(app);
    const event = await createRoundEvent(organizer.accessToken);

    await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(applicant.accessToken))
      .expect(201);

    const res = await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(applicant.accessToken))
      .expect(409);
    expect(res.body.error.code).toBe("CONFLICT");
  });

  test("capacity <= currentの募集への参加申請は409 CONFLICTを返す（capacity>current検証）", async () => {
    const organizer = await registerNewUser(app);
    const a1 = await registerNewUser(app);
    const a2 = await registerNewUser(app);
    const a3 = await registerNewUser(app);
    const event = await createRoundEvent(organizer.accessToken, { capacity: 1 });

    const req1 = await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(a1.accessToken))
      .expect(201);
    await request(app)
      .post(`/round-events/${event.event_id}/join-requests/${req1.body.join_request_id}/approve`)
      .set(...authHeader(organizer.accessToken))
      .expect(200);

    // capacity=1が既に埋まっているので、別ユーザーの新規申請自体が409になる
    const res = await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(a2.accessToken))
      .expect(409);
    expect(res.body.error.code).toBe("CONFLICT");

    void a3; // 予約（未使用変数警告回避、将来の複数人シナリオ拡張用）
  });

  test("GET /round-events/{id}/join-requestsは主催者本人のみ許可され、それ以外は403 FORBIDDENを返す", async () => {
    const organizer = await registerNewUser(app);
    const applicant = await registerNewUser(app);
    const stranger = await registerNewUser(app);
    const event = await createRoundEvent(organizer.accessToken);
    await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(applicant.accessToken))
      .expect(201);

    const forbiddenRes = await request(app)
      .get(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(stranger.accessToken))
      .expect(403);
    expect(forbiddenRes.body.error.code).toBe("FORBIDDEN");

    const okRes = await request(app)
      .get(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(organizer.accessToken))
      .expect(200);
    expect(okRes.body).toHaveLength(1);
    expect(okRes.body[0].user_id).toBe(applicant.userId);
  });

  test("主催者が承認するとcurrentが加算され、status=APPROVED・Connectionが作成される", async () => {
    const organizer = await registerNewUser(app);
    const applicant = await registerNewUser(app);
    const event = await createRoundEvent(organizer.accessToken);
    const joinRes = await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(applicant.accessToken))
      .expect(201);

    const approveRes = await request(app)
      .post(`/round-events/${event.event_id}/join-requests/${joinRes.body.join_request_id}/approve`)
      .set(...authHeader(organizer.accessToken))
      .expect(200);
    expect(approveRes.body.status).toBe("APPROVED");
    expect(approveRes.body.responded_at).not.toBeNull();

    const eventRes = await request(app)
      .get(`/round-events/${event.event_id}`)
      .set(...authHeader(organizer.accessToken))
      .expect(200);
    expect(eventRes.body.current).toBe(1);

    // Connection作成の確認（技術設計書5-2章・12-2-3章、connections/{pairId}への直接Get）
    const pairId = buildPairId(organizer.userId, applicant.userId);
    const connectionSnap = await db.collection("connections").doc(pairId).get();
    expect(connectionSnap.exists).toBe(true);
    const connectionData = connectionSnap.data()!;
    expect(connectionData.sourceType).toBe("ROUND_JOIN");
    expect(connectionData.sourceId).toBe(joinRes.body.join_request_id);
  });

  test("主催者以外が承認しようとすると403 FORBIDDENを返す", async () => {
    const organizer = await registerNewUser(app);
    const applicant = await registerNewUser(app);
    const stranger = await registerNewUser(app);
    const event = await createRoundEvent(organizer.accessToken);
    const joinRes = await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(applicant.accessToken))
      .expect(201);

    const res = await request(app)
      .post(`/round-events/${event.event_id}/join-requests/${joinRes.body.join_request_id}/approve`)
      .set(...authHeader(stranger.accessToken))
      .expect(403);
    expect(res.body.error.code).toBe("FORBIDDEN");
  });

  test("同じ参加申請を二重承認しようとすると2回目は409 CONFLICTを返す（二重承認防止）", async () => {
    const organizer = await registerNewUser(app);
    const applicant = await registerNewUser(app);
    const event = await createRoundEvent(organizer.accessToken);
    const joinRes = await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(applicant.accessToken))
      .expect(201);

    await request(app)
      .post(`/round-events/${event.event_id}/join-requests/${joinRes.body.join_request_id}/approve`)
      .set(...authHeader(organizer.accessToken))
      .expect(200);

    const res = await request(app)
      .post(`/round-events/${event.event_id}/join-requests/${joinRes.body.join_request_id}/approve`)
      .set(...authHeader(organizer.accessToken))
      .expect(409);
    expect(res.body.error.code).toBe("CONFLICT");

    // currentが二重加算されていないことも確認
    const eventRes = await request(app)
      .get(`/round-events/${event.event_id}`)
      .set(...authHeader(organizer.accessToken))
      .expect(200);
    expect(eventRes.body.current).toBe(1);
  });

  test("承認により定員に達した後、別の保留中申請を承認しようとすると409 CONFLICTを返す（承認時のcapacity再検証）", async () => {
    const organizer = await registerNewUser(app);
    const a1 = await registerNewUser(app);
    const a2 = await registerNewUser(app);
    const event = await createRoundEvent(organizer.accessToken, { capacity: 1 });

    const req1 = await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(a1.accessToken))
      .expect(201);

    // capacity=1のためa2の申請自体は先にcurrent=0の状態で行っておく（PENDINGは複数持てる）
    // ただしa2の申請作成時点でcapacity(1) <= current(0)は成立しないため201になる想定。
    const req2 = await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(a2.accessToken))
      .expect(201);

    await request(app)
      .post(`/round-events/${event.event_id}/join-requests/${req1.body.join_request_id}/approve`)
      .set(...authHeader(organizer.accessToken))
      .expect(200);

    const res = await request(app)
      .post(`/round-events/${event.event_id}/join-requests/${req2.body.join_request_id}/approve`)
      .set(...authHeader(organizer.accessToken))
      .expect(409);
    expect(res.body.error.code).toBe("CONFLICT");
  });

  test("主催者が却下するとstatus=REJECTEDになりcurrentは加算されない", async () => {
    const organizer = await registerNewUser(app);
    const applicant = await registerNewUser(app);
    const event = await createRoundEvent(organizer.accessToken);
    const joinRes = await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(applicant.accessToken))
      .expect(201);

    const rejectRes = await request(app)
      .post(`/round-events/${event.event_id}/join-requests/${joinRes.body.join_request_id}/reject`)
      .set(...authHeader(organizer.accessToken))
      .expect(200);
    expect(rejectRes.body.status).toBe("REJECTED");

    const eventRes = await request(app)
      .get(`/round-events/${event.event_id}`)
      .set(...authHeader(organizer.accessToken))
      .expect(200);
    expect(eventRes.body.current).toBe(0);

    // 却下後はConnectionが作成されていないことも確認
    const pairId = buildPairId(organizer.userId, applicant.userId);
    const connectionSnap = await db.collection("connections").doc(pairId).get();
    expect(connectionSnap.exists).toBe(false);
  });

  test("主催者以外が却下しようとすると403 FORBIDDENを返す", async () => {
    const organizer = await registerNewUser(app);
    const applicant = await registerNewUser(app);
    const stranger = await registerNewUser(app);
    const event = await createRoundEvent(organizer.accessToken);
    const joinRes = await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(applicant.accessToken))
      .expect(201);

    const res = await request(app)
      .post(`/round-events/${event.event_id}/join-requests/${joinRes.body.join_request_id}/reject`)
      .set(...authHeader(stranger.accessToken))
      .expect(403);
    expect(res.body.error.code).toBe("FORBIDDEN");
  });

  test("既に却下済みの申請を再度承認しようとすると409 CONFLICTを返す", async () => {
    const organizer = await registerNewUser(app);
    const applicant = await registerNewUser(app);
    const event = await createRoundEvent(organizer.accessToken);
    const joinRes = await request(app)
      .post(`/round-events/${event.event_id}/join-requests`)
      .set(...authHeader(applicant.accessToken))
      .expect(201);

    await request(app)
      .post(`/round-events/${event.event_id}/join-requests/${joinRes.body.join_request_id}/reject`)
      .set(...authHeader(organizer.accessToken))
      .expect(200);

    const res = await request(app)
      .post(`/round-events/${event.event_id}/join-requests/${joinRes.body.join_request_id}/approve`)
      .set(...authHeader(organizer.accessToken))
      .expect(409);
    expect(res.body.error.code).toBe("CONFLICT");
  });
});
