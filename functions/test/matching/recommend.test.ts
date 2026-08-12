import request from "supertest";
import { createApp } from "../../src/app";
import { authHeader, registerNewUser, seedArea } from "../helpers/fixtures";

/**
 * `GET /users/recommend`（技術設計書6-5章、要件定義書3-1章のスコアリング）。
 *
 * 配点はスコア差±10:40点／エリア一致:40点／目的一致:20点で、いずれも20の倍数のため、
 * 到達しうる合計スコアは {0,20,40,60,80,100} のみである（59等の中間値は原理上存在しない）。
 * そのため「合計スコアちょうど60・59」の境界値検証は、実際には「推薦される最小スコア(60)」と
 * 「推薦されない最大スコア(40)」の境界として検証する（本テストファイル内にその旨を明記する）。
 */
describe("GET /users/recommend", () => {
  const app = createApp();

  test("未認証は401 UNAUTHENTICATEDを返す", async () => {
    const res = await request(app).get("/users/recommend").expect(401);
    expect(res.body.error.code).toBe("UNAUTHENTICATED");
  });

  test("自分自身は結果に含まれない", async () => {
    const areaId = await seedArea();
    const me = await registerNewUser(app, { areaId, averageScore: 100, purpose: "CASUAL" });

    const res = await request(app)
      .get("/users/recommend")
      .set(...authHeader(me.accessToken))
      .expect(200);

    expect(res.body.some((u: { user_id: string }) => u.user_id === me.userId)).toBe(false);
  });

  test("スコア差ちょうど10(閾値内)は40点が加算され、目的一致(20点)と合わせて合計60点で推薦される", async () => {
    const areaA = await seedArea();
    const areaB = await seedArea();
    const me = await registerNewUser(app, { areaId: areaA, averageScore: 100, purpose: "CASUAL" });
    // エリア不一致・目的一致・スコア差=10(閾値内) => 0(エリア) + 20(目的) + 40(スコア差) = 60点ちょうど
    const candidate = await registerNewUser(app, { areaId: areaB, averageScore: 110, purpose: "CASUAL" });

    const res = await request(app)
      .get("/users/recommend")
      .set(...authHeader(me.accessToken))
      .expect(200);

    const ids = res.body.map((u: { user_id: string }) => u.user_id);
    expect(ids).toContain(candidate.userId);
  });

  test("スコア差ちょうど11(閾値外)はスコア差の加点が無く、目的一致(20点)のみで合計20点となり推薦されない", async () => {
    const areaA = await seedArea();
    const areaB = await seedArea();
    const me = await registerNewUser(app, { areaId: areaA, averageScore: 100, purpose: "CASUAL" });
    // エリア不一致・目的一致・スコア差=11(閾値外) => 0(エリア) + 20(目的) + 0(スコア差) = 20点
    const candidate = await registerNewUser(app, { areaId: areaB, averageScore: 111, purpose: "CASUAL" });

    const res = await request(app)
      .get("/users/recommend")
      .set(...authHeader(me.accessToken))
      .expect(200);

    const ids = res.body.map((u: { user_id: string }) => u.user_id);
    expect(ids).not.toContain(candidate.userId);
  });

  test("合計スコアがちょうど40点(エリア一致のみ)は推薦されない(60点未満)", async () => {
    const areaA = await seedArea();
    const me = await registerNewUser(app, { areaId: areaA, averageScore: 100, purpose: "CASUAL" });
    // エリア一致(40点)のみ。スコア差が閾値外・目的不一致で他の加点なし => 合計40点
    const candidate = await registerNewUser(app, {
      areaId: areaA,
      averageScore: 200,
      purpose: "SERIOUS",
    });

    const res = await request(app)
      .get("/users/recommend")
      .set(...authHeader(me.accessToken))
      .expect(200);

    const ids = res.body.map((u: { user_id: string }) => u.user_id);
    expect(ids).not.toContain(candidate.userId);
  });

  test("合計スコアがちょうど60点(スコア差+目的一致)は推薦される(60点以上)", async () => {
    const areaA = await seedArea();
    const areaB = await seedArea();
    const me = await registerNewUser(app, { areaId: areaA, averageScore: 100, purpose: "CASUAL" });
    // エリア不一致(0点) + スコア差5(閾値内,40点) + 目的一致(20点) => 合計60点
    const candidate = await registerNewUser(app, { areaId: areaB, averageScore: 105, purpose: "CASUAL" });

    const res = await request(app)
      .get("/users/recommend")
      .set(...authHeader(me.accessToken))
      .expect(200);

    const ids = res.body.map((u: { user_id: string }) => u.user_id);
    expect(ids).toContain(candidate.userId);
  });

  test("3条件すべて一致で合計100点は推薦される", async () => {
    const areaA = await seedArea();
    const me = await registerNewUser(app, { areaId: areaA, averageScore: 100, purpose: "CASUAL" });
    const candidate = await registerNewUser(app, { areaId: areaA, averageScore: 100, purpose: "CASUAL" });

    const res = await request(app)
      .get("/users/recommend")
      .set(...authHeader(me.accessToken))
      .expect(200);

    const found = res.body.find((u: { user_id: string }) => u.user_id === candidate.userId);
    expect(found).toBeDefined();
  });

  test("条件を一切満たさない(合計0点)ユーザーは推薦されない", async () => {
    const areaA = await seedArea();
    const areaB = await seedArea();
    const me = await registerNewUser(app, { areaId: areaA, averageScore: 100, purpose: "CASUAL" });
    const candidate = await registerNewUser(app, {
      areaId: areaB,
      averageScore: 200,
      purpose: "LESSON_WANTED",
    });

    const res = await request(app)
      .get("/users/recommend")
      .set(...authHeader(me.accessToken))
      .expect(200);

    const ids = res.body.map((u: { user_id: string }) => u.user_id);
    expect(ids).not.toContain(candidate.userId);
  });

  test("結果はスコア降順でソートされる(DeveloperAgent実装判断、技術設計書6-5章に順序の明記は無い)", async () => {
    const areaA = await seedArea();
    const areaB = await seedArea();
    const me = await registerNewUser(app, { areaId: areaA, averageScore: 100, purpose: "CASUAL" });
    // 60点(スコア差+目的一致)
    const low = await registerNewUser(app, { areaId: areaB, averageScore: 105, purpose: "CASUAL" });
    // 100点(全一致)
    const high = await registerNewUser(app, { areaId: areaA, averageScore: 100, purpose: "CASUAL" });
    // 80点(スコア差+エリア一致、目的不一致)
    const mid = await registerNewUser(app, { areaId: areaA, averageScore: 100, purpose: "SERIOUS" });

    const res = await request(app)
      .get("/users/recommend")
      .set(...authHeader(me.accessToken))
      .expect(200);

    const ids = res.body.map((u: { user_id: string }) => u.user_id);
    const order = [high.userId, mid.userId, low.userId].filter((id) => ids.includes(id));
    const positions = order.map((id) => ids.indexOf(id));
    const sorted = [...positions].sort((a, b) => a - b);
    expect(positions).toEqual(sorted);
  });
});
