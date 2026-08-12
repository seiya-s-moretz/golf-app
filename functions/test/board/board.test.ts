import request from "supertest";
import { createApp } from "../../src/app";
import { authHeader, registerNewUser } from "../helpers/fixtures";

/**
 * `GET /board`・`POST /board`（技術設計書6-6章）。
 * `GET /board`はページネーション無しで全件取得する実装判断（DeveloperAgent、board.service.tsコメント参照）。
 * 技術設計書6-6章にはページネーションの明記が無く、Androidクライアント`ApiService.getBoardPosts()`も
 * クエリパラメータを取らないため、実装判断として妥当と判断する（TesterAgent確認）。
 */
describe("GET/POST /board", () => {
  const app = createApp();

  test("GET /boardは未認証だと401 UNAUTHENTICATEDを返す", async () => {
    const res = await request(app).get("/board").expect(401);
    expect(res.body.error.code).toBe("UNAUTHENTICATED");
  });

  test("POST /boardは未認証だと401 UNAUTHENTICATEDを返す", async () => {
    const res = await request(app).post("/board").send({ content: "テスト投稿" }).expect(401);
    expect(res.body.error.code).toBe("UNAUTHENTICATED");
  });

  test("投稿を作成すると201でpost_id・user_id・content・created_atが返る", async () => {
    const user = await registerNewUser(app);
    const res = await request(app)
      .post("/board")
      .set(...authHeader(user.accessToken))
      .send({ content: "今週末ラウンドご一緒しませんか" })
      .expect(201);

    expect(res.body.post_id).toBeDefined();
    expect(res.body.user_id).toBe(user.userId);
    expect(res.body.content).toBe("今週末ラウンドご一緒しませんか");
    expect(res.body.created_at).toBeDefined();
  });

  test("contentが空文字の投稿は400 VALIDATION_ERRORを返す", async () => {
    const user = await registerNewUser(app);
    const res = await request(app)
      .post("/board")
      .set(...authHeader(user.accessToken))
      .send({ content: "" })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("GET /boardは全ユーザーの投稿を作成日時降順で返す(ページネーション無し、全件取得)", async () => {
    const userA = await registerNewUser(app);
    const userB = await registerNewUser(app);

    const post1 = await request(app)
      .post("/board")
      .set(...authHeader(userA.accessToken))
      .send({ content: "1件目" })
      .expect(201);
    const post2 = await request(app)
      .post("/board")
      .set(...authHeader(userB.accessToken))
      .send({ content: "2件目" })
      .expect(201);

    const res = await request(app)
      .get("/board")
      .set(...authHeader(userA.accessToken))
      .expect(200);

    expect(res.body).toHaveLength(2);
    // created_at降順(新しい投稿が先頭)
    expect(res.body[0].post_id).toBe(post2.body.post_id);
    expect(res.body[1].post_id).toBe(post1.body.post_id);
  });

  test("投稿が0件の場合は空配列を返す", async () => {
    const user = await registerNewUser(app);
    const res = await request(app)
      .get("/board")
      .set(...authHeader(user.accessToken))
      .expect(200);
    expect(res.body).toEqual([]);
  });
});
