import request from "supertest";
import { createApp } from "../../src/app";
import { db } from "../../src/config/firebaseAdmin";
import { buildPairId } from "../../src/lib/pairId";
import { MESSAGE_MAX_LENGTH } from "../../src/modules/messaging/messaging.validation";
import { authHeader, establishConnection, registerNewUser } from "../helpers/fixtures";

/**
 * `GET /conversations`・`GET/POST /conversations/{partnerId}/messages`・
 * `POST /conversations/{partnerId}/read`（技術設計書6-7章）。
 *
 * メッセージ本文の最大文字数(500)は技術設計書5-2章に具体的な明記が無く、DeveloperAgentの実装判断
 * （`messaging.validation.ts`のコメント参照）。500文字という数値自体は要件定義書・技術設計書のどこにも
 * 矛盾する記載が無く、一般的なチャットメッセージの上限として不合理ではないため、TesterAgentとしては
 * 「バグではない・実装判断として許容」と評価し、境界値(500文字はOK、501文字は400)のみ確認する。
 */
describe("メッセージ機能", () => {
  const app = createApp();

  describe("GET /conversations", () => {
    test("未認証は401 UNAUTHENTICATEDを返す", async () => {
      const res = await request(app).get("/conversations").expect(401);
      expect(res.body.error.code).toBe("UNAUTHENTICATED");
    });

    test("Connectionが無い場合は空配列を返す", async () => {
      const me = await registerNewUser(app);
      const res = await request(app)
        .get("/conversations")
        .set(...authHeader(me.accessToken))
        .expect(200);
      expect(res.body).toEqual([]);
    });

    test("Connectionはあるがメッセージが1件も無い場合はlast_message=null・unread_count=0で返る", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      await establishConnection(app, userA, userB);

      const res = await request(app)
        .get("/conversations")
        .set(...authHeader(userA.accessToken))
        .expect(200);
      expect(res.body).toHaveLength(1);
      expect(res.body[0].partner.user_id).toBe(userB.userId);
      expect(res.body[0].last_message).toBeNull();
      expect(res.body[0].unread_count).toBe(0);
    });
  });

  describe("POST /conversations/{partnerId}/messages", () => {
    test("未認証は401 UNAUTHENTICATEDを返す", async () => {
      const res = await request(app).post("/conversations/dummy-id/messages").send({ content: "hi" }).expect(401);
      expect(res.body.error.code).toBe("UNAUTHENTICATED");
    });

    test("Connectionが存在しない相手への送信は403 FORBIDDENを返す", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      const res = await request(app)
        .post(`/conversations/${userB.userId}/messages`)
        .set(...authHeader(userA.accessToken))
        .send({ content: "こんにちは" })
        .expect(403);
      expect(res.body.error.code).toBe("FORBIDDEN");
    });

    test("自分自身への送信は400 VALIDATION_ERRORを返す", async () => {
      const me = await registerNewUser(app);
      const res = await request(app)
        .post(`/conversations/${me.userId}/messages`)
        .set(...authHeader(me.accessToken))
        .send({ content: "独り言" })
        .expect(400);
      expect(res.body.error.code).toBe("VALIDATION_ERROR");
    });

    test("存在しない相手への送信は404 NOT_FOUNDを返す", async () => {
      const me = await registerNewUser(app);
      const res = await request(app)
        .post("/conversations/存在しないユーザーID/messages")
        .set(...authHeader(me.accessToken))
        .send({ content: "こんにちは" })
        .expect(404);
      expect(res.body.error.code).toBe("NOT_FOUND");
    });

    test("contentが空文字だと400 VALIDATION_ERRORを返す", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      await establishConnection(app, userA, userB);
      const res = await request(app)
        .post(`/conversations/${userB.userId}/messages`)
        .set(...authHeader(userA.accessToken))
        .send({ content: "" })
        .expect(400);
      expect(res.body.error.code).toBe("VALIDATION_ERROR");
    });

    test(`content ${MESSAGE_MAX_LENGTH}文字ちょうどは送信できる(境界値)`, async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      await establishConnection(app, userA, userB);
      const content = "あ".repeat(MESSAGE_MAX_LENGTH);
      const res = await request(app)
        .post(`/conversations/${userB.userId}/messages`)
        .set(...authHeader(userA.accessToken))
        .send({ content })
        .expect(201);
      expect(res.body.content).toBe(content);
    });

    test(`content ${MESSAGE_MAX_LENGTH + 1}文字は400 VALIDATION_ERRORを返す(境界値)`, async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      await establishConnection(app, userA, userB);
      const content = "あ".repeat(MESSAGE_MAX_LENGTH + 1);
      const res = await request(app)
        .post(`/conversations/${userB.userId}/messages`)
        .set(...authHeader(userA.accessToken))
        .send({ content })
        .expect(400);
      expect(res.body.error.code).toBe("VALIDATION_ERROR");
    });

    test("正常送信は201でsender_id・content・user_a_id/user_b_id・read_at=nullを返す", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      await establishConnection(app, userA, userB);
      const res = await request(app)
        .post(`/conversations/${userB.userId}/messages`)
        .set(...authHeader(userA.accessToken))
        .send({ content: "今度ラウンドどうですか" })
        .expect(201);

      expect(res.body.sender_id).toBe(userA.userId);
      expect(res.body.content).toBe("今度ラウンドどうですか");
      expect(res.body.read_at).toBeNull();
      expect([res.body.user_a_id, res.body.user_b_id].sort()).toEqual([userA.userId, userB.userId].sort());
    });

    test("ブロック関係にある相手への送信は403 BLOCKEDを返す(自分が相手をブロック)", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      await establishConnection(app, userA, userB);
      await request(app)
        .post(`/users/${userB.userId}/block`)
        .set(...authHeader(userA.accessToken))
        .expect(204);

      const res = await request(app)
        .post(`/conversations/${userB.userId}/messages`)
        .set(...authHeader(userA.accessToken))
        .send({ content: "届かないはず" })
        .expect(403);
      expect(res.body.error.code).toBe("BLOCKED");
    });

    test("ブロック関係にある相手への送信は403 BLOCKEDを返す(相手が自分をブロック、双方向)", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      await establishConnection(app, userA, userB);
      await request(app)
        .post(`/users/${userA.userId}/block`)
        .set(...authHeader(userB.accessToken))
        .expect(204);

      const res = await request(app)
        .post(`/conversations/${userB.userId}/messages`)
        .set(...authHeader(userA.accessToken))
        .send({ content: "届かないはず" })
        .expect(403);
      expect(res.body.error.code).toBe("BLOCKED");
    });
  });

  describe("GET /conversations/{partnerId}/messages（一覧・ページネーション）", () => {
    test("Connectionが存在しない相手には403 FORBIDDENを返す", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      const res = await request(app)
        .get(`/conversations/${userB.userId}/messages`)
        .set(...authHeader(userA.accessToken))
        .expect(403);
      expect(res.body.error.code).toBe("FORBIDDEN");
    });

    test("送受信したメッセージをcreated_at降順で返す", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      await establishConnection(app, userA, userB);

      const m1 = await request(app)
        .post(`/conversations/${userB.userId}/messages`)
        .set(...authHeader(userA.accessToken))
        .send({ content: "1通目" })
        .expect(201);
      const m2 = await request(app)
        .post(`/conversations/${userA.userId}/messages`)
        .set(...authHeader(userB.accessToken))
        .send({ content: "2通目" })
        .expect(201);

      const res = await request(app)
        .get(`/conversations/${userB.userId}/messages`)
        .set(...authHeader(userA.accessToken))
        .expect(200);
      expect(res.body).toHaveLength(2);
      expect(res.body[0].message_id).toBe(m2.body.message_id);
      expect(res.body[1].message_id).toBe(m1.body.message_id);
    });

    test("limitを指定すると件数が絞り込まれ、beforeカーソルで続きを取得できる", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      await establishConnection(app, userA, userB);

      const sent: Array<{ body: Record<string, unknown> }> = [];
      for (let i = 0; i < 5; i += 1) {
        // eslint-disable-next-line no-await-in-loop
        const res = await request(app)
          .post(`/conversations/${userB.userId}/messages`)
          .set(...authHeader(userA.accessToken))
          .send({ content: `メッセージ${i}` })
          .expect(201);
        sent.push(res);
      }

      const firstPage = await request(app)
        .get(`/conversations/${userB.userId}/messages?limit=2`)
        .set(...authHeader(userA.accessToken))
        .expect(200);
      expect(firstPage.body).toHaveLength(2);
      expect(firstPage.body[0].content).toBe("メッセージ4");
      expect(firstPage.body[1].content).toBe("メッセージ3");

      const cursor = firstPage.body[1].created_at as string;
      const secondPage = await request(app)
        .get(`/conversations/${userB.userId}/messages?limit=2&before=${encodeURIComponent(cursor)}`)
        .set(...authHeader(userA.accessToken))
        .expect(200);
      expect(secondPage.body).toHaveLength(2);
      expect(secondPage.body[0].content).toBe("メッセージ2");
      expect(secondPage.body[1].content).toBe("メッセージ1");
    });
  });

  describe("POST /conversations/{partnerId}/read", () => {
    test("Connectionが存在しない場合は403 FORBIDDENを返す", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      const res = await request(app)
        .post(`/conversations/${userB.userId}/read`)
        .set(...authHeader(userA.accessToken))
        .expect(403);
      expect(res.body.error.code).toBe("FORBIDDEN");
    });

    test("既読化すると相手からの未読メッセージのread_atが更新され、unread_countが0になる", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      await establishConnection(app, userA, userB);

      await request(app)
        .post(`/conversations/${userA.userId}/messages`)
        .set(...authHeader(userB.accessToken))
        .send({ content: "既読テスト" })
        .expect(201);

      const before = await request(app)
        .get("/conversations")
        .set(...authHeader(userA.accessToken))
        .expect(200);
      expect(before.body[0].unread_count).toBe(1);
      expect(before.body[0].last_message.read_at).toBeNull();

      await request(app)
        .post(`/conversations/${userB.userId}/read`)
        .set(...authHeader(userA.accessToken))
        .expect(204);

      const after = await request(app)
        .get("/conversations")
        .set(...authHeader(userA.accessToken))
        .expect(200);
      expect(after.body[0].unread_count).toBe(0);
      expect(after.body[0].last_message.read_at).not.toBeNull();

      const pairId = buildPairId(userA.userId, userB.userId);
      const messagesSnap = await db.collection("messages").where("pairId", "==", pairId).get();
      messagesSnap.docs.forEach((d) => {
        expect(d.data().readAt).not.toBeNull();
      });
    });

    test("自分が送ったメッセージの未読件数(相手側)には影響しない", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      await establishConnection(app, userA, userB);

      await request(app)
        .post(`/conversations/${userB.userId}/messages`)
        .set(...authHeader(userA.accessToken))
        .send({ content: "Aから送信" })
        .expect(201);

      // Aが自分自身宛の会話を既読化しても、Bの未読件数は変わらない
      await request(app)
        .post(`/conversations/${userB.userId}/read`)
        .set(...authHeader(userA.accessToken))
        .expect(204);

      const bView = await request(app)
        .get("/conversations")
        .set(...authHeader(userB.accessToken))
        .expect(200);
      expect(bView.body[0].unread_count).toBe(1);
    });
  });

  describe("Connectionへの非正規化フィールドの整合性（技術設計書12-2-3章）", () => {
    test("複数往復のやり取り後もlast_message・unread_countが双方の視点で正しい", async () => {
      const userA = await registerNewUser(app);
      const userB = await registerNewUser(app);
      await establishConnection(app, userA, userB);

      await request(app)
        .post(`/conversations/${userB.userId}/messages`)
        .set(...authHeader(userA.accessToken))
        .send({ content: "A1" })
        .expect(201);
      await request(app)
        .post(`/conversations/${userA.userId}/messages`)
        .set(...authHeader(userB.accessToken))
        .send({ content: "B1" })
        .expect(201);
      await request(app)
        .post(`/conversations/${userB.userId}/messages`)
        .set(...authHeader(userA.accessToken))
        .send({ content: "A2(最新)" })
        .expect(201);

      const aView = await request(app)
        .get("/conversations")
        .set(...authHeader(userA.accessToken))
        .expect(200);
      expect(aView.body[0].last_message.content).toBe("A2(最新)");
      expect(aView.body[0].last_message.sender_id).toBe(userA.userId);
      // Aは自分が最後に送ったので未読はB1の1件を既に読んだ状態(自分が送信した時点でBからのB1を読んだことにはならないが、
      // Aが最後に取得していない新着はB1のみ。B1は既にAが2通目送信前にはunread=1のはず。ここではAPIの一貫性のみ確認する)
      expect(aView.body[0].unread_count).toBe(1); // B1がまだ未読

      const bView = await request(app)
        .get("/conversations")
        .set(...authHeader(userB.accessToken))
        .expect(200);
      expect(bView.body[0].last_message.content).toBe("A2(最新)");
      expect(bView.body[0].unread_count).toBe(2); // A1, A2の2件が未読
    });
  });
});
