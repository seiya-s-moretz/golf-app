import request from "supertest";
import { createApp } from "../../src/app";
import { db } from "../../src/config/firebaseAdmin";
import { buildPairId } from "../../src/lib/pairId";
import { authHeader, registerNewUser } from "../helpers/fixtures";

/**
 * `POST /users/{id}/match-requests`・`GET /users/me/match-requests`・
 * `POST /match-requests/{id}/approve`・`POST /match-requests/{id}/reject`（技術設計書6-5章）。
 */
describe("マッチング申請フロー", () => {
  const app = createApp();

  test("未認証は401 UNAUTHENTICATEDを返す", async () => {
    const res = await request(app).post("/users/dummy-id/match-requests").expect(401);
    expect(res.body.error.code).toBe("UNAUTHENTICATED");
  });

  test("自分自身への申請は400 VALIDATION_ERRORを返す", async () => {
    const me = await registerNewUser(app);
    const res = await request(app)
      .post(`/users/${me.userId}/match-requests`)
      .set(...authHeader(me.accessToken))
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("存在しないユーザーへの申請は404 NOT_FOUNDを返す", async () => {
    const me = await registerNewUser(app);
    const res = await request(app)
      .post("/users/存在しないユーザーID/match-requests")
      .set(...authHeader(me.accessToken))
      .expect(404);
    expect(res.body.error.code).toBe("NOT_FOUND");
  });

  test("正常な申請はPENDINGで201を返す", async () => {
    const from = await registerNewUser(app);
    const to = await registerNewUser(app);
    const res = await request(app)
      .post(`/users/${to.userId}/match-requests`)
      .set(...authHeader(from.accessToken))
      .expect(201);
    expect(res.body.status).toBe("PENDING");
    expect(res.body.from_user_id).toBe(from.userId);
    expect(res.body.to_user_id).toBe(to.userId);
    expect(res.body.responded_at).toBeNull();
  });

  test("同一方向(from→to)へのPENDING重複申請は409 CONFLICTを返す", async () => {
    const from = await registerNewUser(app);
    const to = await registerNewUser(app);
    await request(app)
      .post(`/users/${to.userId}/match-requests`)
      .set(...authHeader(from.accessToken))
      .expect(201);

    const res = await request(app)
      .post(`/users/${to.userId}/match-requests`)
      .set(...authHeader(from.accessToken))
      .expect(409);
    expect(res.body.error.code).toBe("CONFLICT");
  });

  /**
   * 【要確認・設計判断】技術設計書5-2章のMatchRequest制約は文言上「(from_user_id, to_user_id)の
   * 組み合わせでPENDING状態は1件まで」と、あくまで方向付きの組(ordered pair)として明記されている。
   * 実装(`matching.service.ts`のcreateMatchRequest)もこの文言どおり方向別のみで重複チェックしており、
   * 技術設計書の記載そのものには一致する（バグではない）。
   *
   * 一方で、逆方向(相手からも既に申請が来ている場合)は考慮されておらず、AさんがBさんに申請し、
   * 同時にBさんもAさんに申請するとPENDINGなMatchRequestが2件同時に存在しうる。要件定義書3-1章・
   * 技術設計書6-5章のどちらにも「双方向からの申請が来た場合に自動で相互マッチ扱いにする」等の
   * 挙動は明記されていないため、この2件併存自体が仕様違反とまでは言えない。ただし、両者が
   * 個別にapprove操作をすると、Connectionは冪等生成(`ensureConnection`)のため実害はない
   * （2回approveしても`connections/{pairId}`は1件のみ）ものの、UI/UX上は「既に相手から申請が
   * 来ている場合はマッチング申請ボタンではなく承認ボタンを出す」等の考慮がAndroidクライアント側に
   * 必要になりうる（クライアント側の実装確認はTesterAgentの担当範囲外、ArchitectAgent/DeveloperAgentへの
   * 確認事項として記録）。本テストは「技術設計書の文言通りに実装されている」ことを確認する目的で
   * 現状挙動を明文化する（Pass=現状挙動）。
   */
  test("【設計確認】逆方向(to→from)の申請は独立して扱われ、PENDINGが双方向で同時に存在しうる", async () => {
    const userA = await registerNewUser(app);
    const userB = await registerNewUser(app);

    await request(app)
      .post(`/users/${userB.userId}/match-requests`)
      .set(...authHeader(userA.accessToken))
      .expect(201);

    // Bから見ればAはまだ自分に申請していないので、B→Aの新規申請はブロックされない
    const res = await request(app)
      .post(`/users/${userA.userId}/match-requests`)
      .set(...authHeader(userB.accessToken))
      .expect(201);
    expect(res.body.from_user_id).toBe(userB.userId);
    expect(res.body.to_user_id).toBe(userA.userId);

    const receivedByA = await request(app)
      .get("/users/me/match-requests?direction=received")
      .set(...authHeader(userA.accessToken))
      .expect(200);
    expect(receivedByA.body).toHaveLength(1);
    expect(receivedByA.body[0].from_user_id).toBe(userB.userId);

    const sentByA = await request(app)
      .get("/users/me/match-requests?direction=sent")
      .set(...authHeader(userA.accessToken))
      .expect(200);
    expect(sentByA.body).toHaveLength(1);
    expect(sentByA.body[0].to_user_id).toBe(userB.userId);
  });

  test("PENDING以外(却下済み)の同一方向申請は再申請でき409にならない", async () => {
    const from = await registerNewUser(app);
    const to = await registerNewUser(app);
    const first = await request(app)
      .post(`/users/${to.userId}/match-requests`)
      .set(...authHeader(from.accessToken))
      .expect(201);
    await request(app)
      .post(`/match-requests/${first.body.match_request_id}/reject`)
      .set(...authHeader(to.accessToken))
      .expect(200);

    const res = await request(app)
      .post(`/users/${to.userId}/match-requests`)
      .set(...authHeader(from.accessToken))
      .expect(201);
    expect(res.body.status).toBe("PENDING");
  });

  describe("GET /users/me/match-requests?direction=", () => {
    test("directionが不正な値だと400 VALIDATION_ERRORを返す", async () => {
      const me = await registerNewUser(app);
      const res = await request(app)
        .get("/users/me/match-requests?direction=invalid")
        .set(...authHeader(me.accessToken))
        .expect(400);
      expect(res.body.error.code).toBe("VALIDATION_ERROR");
    });

    test("directionが未指定だと400 VALIDATION_ERRORを返す", async () => {
      const me = await registerNewUser(app);
      const res = await request(app)
        .get("/users/me/match-requests")
        .set(...authHeader(me.accessToken))
        .expect(400);
      expect(res.body.error.code).toBe("VALIDATION_ERROR");
    });

    test("received/sentで方向別に正しく絞り込まれる", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      const userC = await registerNewUser(app);

      // A→B, C→A
      await request(app)
        .post(`/users/${userB.userId}/match-requests`)
        .set(...authHeader(userA.accessToken))
        .expect(201);
      await request(app)
        .post(`/users/${userA.userId}/match-requests`)
        .set(...authHeader(userC.accessToken))
        .expect(201);

      const received = await request(app)
        .get("/users/me/match-requests?direction=received")
        .set(...authHeader(userA.accessToken))
        .expect(200);
      expect(received.body).toHaveLength(1);
      expect(received.body[0].from_user_id).toBe(userC.userId);

      const sent = await request(app)
        .get("/users/me/match-requests?direction=sent")
        .set(...authHeader(userA.accessToken))
        .expect(200);
      expect(sent.body).toHaveLength(1);
      expect(sent.body[0].to_user_id).toBe(userB.userId);
    });
  });

  describe("POST /match-requests/{id}/approve, /reject", () => {
    test("宛先(to_user_id)本人以外が承認しようとすると403 FORBIDDENを返す", async () => {
      const from = await registerNewUser(app);
      const to = await registerNewUser(app);
      const stranger = await registerNewUser(app);
      const created = await request(app)
        .post(`/users/${to.userId}/match-requests`)
        .set(...authHeader(from.accessToken))
        .expect(201);

      const res = await request(app)
        .post(`/match-requests/${created.body.match_request_id}/approve`)
        .set(...authHeader(stranger.accessToken))
        .expect(403);
      expect(res.body.error.code).toBe("FORBIDDEN");
    });

    test("申請者本人(from_user_id)自身も承認できない(宛先本人のみ許可)", async () => {
      const from = await registerNewUser(app);
      const to = await registerNewUser(app);
      const created = await request(app)
        .post(`/users/${to.userId}/match-requests`)
        .set(...authHeader(from.accessToken))
        .expect(201);

      const res = await request(app)
        .post(`/match-requests/${created.body.match_request_id}/approve`)
        .set(...authHeader(from.accessToken))
        .expect(403);
      expect(res.body.error.code).toBe("FORBIDDEN");
    });

    test("宛先本人が承認するとACCEPTEDになりConnectionが作成される", async () => {
      const from = await registerNewUser(app);
      const to = await registerNewUser(app);
      const created = await request(app)
        .post(`/users/${to.userId}/match-requests`)
        .set(...authHeader(from.accessToken))
        .expect(201);

      const res = await request(app)
        .post(`/match-requests/${created.body.match_request_id}/approve`)
        .set(...authHeader(to.accessToken))
        .expect(200);
      expect(res.body.status).toBe("ACCEPTED");
      expect(res.body.responded_at).not.toBeNull();

      const pairId = buildPairId(from.userId, to.userId);
      const connectionSnap = await db.collection("connections").doc(pairId).get();
      expect(connectionSnap.exists).toBe(true);
      const connectionData = connectionSnap.data()!;
      expect(connectionData.sourceType).toBe("MATCH_REQUEST");
      expect(connectionData.sourceId).toBe(created.body.match_request_id);
    });

    test("宛先本人以外が却下しようとすると403 FORBIDDENを返す", async () => {
      const from = await registerNewUser(app);
      const to = await registerNewUser(app);
      const stranger = await registerNewUser(app);
      const created = await request(app)
        .post(`/users/${to.userId}/match-requests`)
        .set(...authHeader(from.accessToken))
        .expect(201);

      const res = await request(app)
        .post(`/match-requests/${created.body.match_request_id}/reject`)
        .set(...authHeader(stranger.accessToken))
        .expect(403);
      expect(res.body.error.code).toBe("FORBIDDEN");
    });

    test("宛先本人が却下するとREJECTEDになりConnectionは作成されない", async () => {
      const from = await registerNewUser(app);
      const to = await registerNewUser(app);
      const created = await request(app)
        .post(`/users/${to.userId}/match-requests`)
        .set(...authHeader(from.accessToken))
        .expect(201);

      const res = await request(app)
        .post(`/match-requests/${created.body.match_request_id}/reject`)
        .set(...authHeader(to.accessToken))
        .expect(200);
      expect(res.body.status).toBe("REJECTED");

      const pairId = buildPairId(from.userId, to.userId);
      const connectionSnap = await db.collection("connections").doc(pairId).get();
      expect(connectionSnap.exists).toBe(false);
    });

    test("既に処理済みの申請を再度承認しようとすると409 CONFLICTを返す", async () => {
      const from = await registerNewUser(app);
      const to = await registerNewUser(app);
      const created = await request(app)
        .post(`/users/${to.userId}/match-requests`)
        .set(...authHeader(from.accessToken))
        .expect(201);
      await request(app)
        .post(`/match-requests/${created.body.match_request_id}/approve`)
        .set(...authHeader(to.accessToken))
        .expect(200);

      const res = await request(app)
        .post(`/match-requests/${created.body.match_request_id}/approve`)
        .set(...authHeader(to.accessToken))
        .expect(409);
      expect(res.body.error.code).toBe("CONFLICT");
    });

    test("存在しない申請IDへの承認は404 NOT_FOUNDを返す", async () => {
      const me = await registerNewUser(app);
      const res = await request(app)
        .post("/match-requests/存在しない申請ID/approve")
        .set(...authHeader(me.accessToken))
        .expect(404);
      expect(res.body.error.code).toBe("NOT_FOUND");
    });

    test("match-requests配下は未認証だと401 UNAUTHENTICATEDを返す", async () => {
      const res = await request(app).post("/match-requests/dummy-id/approve").expect(401);
      expect(res.body.error.code).toBe("UNAUTHENTICATED");
    });
  });
});
