import request from "supertest";
import { createApp } from "../../src/app";
import { authHeader, registerNewUser } from "../helpers/fixtures";

/**
 * `POST /users/{id}/block`・`DELETE /users/{id}/block`・`GET /users/me/blocks`（技術設計書6-3章・6-8章）。
 * `blocks.service.ts`のコメントのとおり、重複ブロック・存在しないブロック解除も冪等にエラーとしない
 * 実装判断（6章に重複時のエラー要件の明記が無く、UX上も再ブロックをエラーにする必要は薄いと判断されている）。
 * この判断は要件定義書・技術設計書に矛盾する記載が無く、他の多くのAPI（例:GETの空配列許容）とも
 * 一貫した「べき等な操作は成功させる」設計であり、TesterAgentとしても妥当と評価する。
 */
describe("ブロック機能", () => {
  const app = createApp();

  describe("POST /users/{id}/block", () => {
    test("未認証は401 UNAUTHENTICATEDを返す", async () => {
      const res = await request(app).post("/users/dummy-id/block").expect(401);
      expect(res.body.error.code).toBe("UNAUTHENTICATED");
    });

    test("自分自身のブロックは400 VALIDATION_ERRORを返す", async () => {
      const me = await registerNewUser(app);
      const res = await request(app)
        .post(`/users/${me.userId}/block`)
        .set(...authHeader(me.accessToken))
        .expect(400);
      expect(res.body.error.code).toBe("VALIDATION_ERROR");
    });

    test("存在しないユーザーのブロックは404 NOT_FOUNDを返す", async () => {
      const me = await registerNewUser(app);
      const res = await request(app)
        .post("/users/存在しないユーザーID/block")
        .set(...authHeader(me.accessToken))
        .expect(404);
      expect(res.body.error.code).toBe("NOT_FOUND");
    });

    test("正常なブロックは204を返し、GET /users/me/blocksに反映される", async () => {
      const me = await registerNewUser(app);
      const target = await registerNewUser(app);
      await request(app)
        .post(`/users/${target.userId}/block`)
        .set(...authHeader(me.accessToken))
        .expect(204);

      const res = await request(app)
        .get("/users/me/blocks")
        .set(...authHeader(me.accessToken))
        .expect(200);
      expect(res.body).toHaveLength(1);
      expect(res.body[0].user_id).toBe(target.userId);
    });

    test("同じユーザーへの重複ブロックはエラーにならず冪等に成功する", async () => {
      const me = await registerNewUser(app);
      const target = await registerNewUser(app);
      await request(app)
        .post(`/users/${target.userId}/block`)
        .set(...authHeader(me.accessToken))
        .expect(204);
      await request(app)
        .post(`/users/${target.userId}/block`)
        .set(...authHeader(me.accessToken))
        .expect(204);

      const res = await request(app)
        .get("/users/me/blocks")
        .set(...authHeader(me.accessToken))
        .expect(200);
      // 重複ブロックしても一覧には1件のみ(ドキュメントIDがblocker_blocked固定のため上書き)
      expect(res.body).toHaveLength(1);
    });
  });

  describe("DELETE /users/{id}/block", () => {
    test("未認証は401 UNAUTHENTICATEDを返す", async () => {
      const res = await request(app).delete("/users/dummy-id/block").expect(401);
      expect(res.body.error.code).toBe("UNAUTHENTICATED");
    });

    test("ブロックしていない相手への解除リクエストもエラーにならず204を返す(冪等)", async () => {
      const me = await registerNewUser(app);
      const target = await registerNewUser(app);
      await request(app)
        .delete(`/users/${target.userId}/block`)
        .set(...authHeader(me.accessToken))
        .expect(204);
    });

    test("ブロック済みの相手を解除すると一覧から消える", async () => {
      const me = await registerNewUser(app);
      const target = await registerNewUser(app);
      await request(app)
        .post(`/users/${target.userId}/block`)
        .set(...authHeader(me.accessToken))
        .expect(204);
      await request(app)
        .delete(`/users/${target.userId}/block`)
        .set(...authHeader(me.accessToken))
        .expect(204);

      const res = await request(app)
        .get("/users/me/blocks")
        .set(...authHeader(me.accessToken))
        .expect(200);
      expect(res.body).toEqual([]);
    });

    test("解除済みのものをもう一度解除してもエラーにならない(冪等)", async () => {
      const me = await registerNewUser(app);
      const target = await registerNewUser(app);
      await request(app)
        .post(`/users/${target.userId}/block`)
        .set(...authHeader(me.accessToken))
        .expect(204);
      await request(app)
        .delete(`/users/${target.userId}/block`)
        .set(...authHeader(me.accessToken))
        .expect(204);
      await request(app)
        .delete(`/users/${target.userId}/block`)
        .set(...authHeader(me.accessToken))
        .expect(204);
    });
  });

  describe("GET /users/me/blocks", () => {
    test("未認証は401 UNAUTHENTICATEDを返す", async () => {
      const res = await request(app).get("/users/me/blocks").expect(401);
      expect(res.body.error.code).toBe("UNAUTHENTICATED");
    });

    test("何もブロックしていない場合は空配列を返す", async () => {
      const me = await registerNewUser(app);
      const res = await request(app)
        .get("/users/me/blocks")
        .set(...authHeader(me.accessToken))
        .expect(200);
      expect(res.body).toEqual([]);
    });

    test("自分がブロックした相手のみを返す(相手からブロックされているだけの相手は含まない)", async () => {
      const me = await registerNewUser(app);
      const blockedByMe = await registerNewUser(app);
      const blockingMe = await registerNewUser(app);

      await request(app)
        .post(`/users/${blockedByMe.userId}/block`)
        .set(...authHeader(me.accessToken))
        .expect(204);
      await request(app)
        .post(`/users/${me.userId}/block`)
        .set(...authHeader(blockingMe.accessToken))
        .expect(204);

      const res = await request(app)
        .get("/users/me/blocks")
        .set(...authHeader(me.accessToken))
        .expect(200);
      expect(res.body).toHaveLength(1);
      expect(res.body[0].user_id).toBe(blockedByMe.userId);
    });
  });
});
