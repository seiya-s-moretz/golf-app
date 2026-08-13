import request from "supertest";
import { createApp } from "../../src/app";
import { db } from "../../src/config/firebaseAdmin";
import { buildPairId } from "../../src/lib/pairId";
import { authHeader, establishConnection, registerNewUser } from "../helpers/fixtures";

/**
 * 既読化（`POST /conversations/{partnerId}/read`）と会話一覧のブロック除外（技術設計書6-7章・5-2章）。
 *
 * 未読件数は`Connection`の非正規化カウンタだけを見て表示され、再計算する経路が無い。
 * そのため既読化で「0を書き込む」実装だと、既読化の最中に届いた新着が**二度と未読として数えられない**。
 * 「既読にした件数だけ減らす」実装になっていることを、既読化後に届いたメッセージで検証する。
 */
describe("会話の既読化とブロック除外", () => {
  const app = createApp();

  async function connectedPair() {
    const me = await registerNewUser(app);
    const partner = await registerNewUser(app);
    await establishConnection(app, me, partner);
    return { me, partner };
  }

  async function sendMessage(from: { accessToken: string }, toUserId: string, content: string) {
    return request(app)
      .post(`/conversations/${toUserId}/messages`)
      .set(...authHeader(from.accessToken))
      .send({ content })
      .expect(201);
  }

  test("既読化すると相手からの未読が既読になり未読件数が0になる", async () => {
    const { me, partner } = await connectedPair();
    await sendMessage(partner, me.userId, "こんにちは");
    await sendMessage(partner, me.userId, "よろしくお願いします");

    await request(app)
      .post(`/conversations/${partner.userId}/read`)
      .set(...authHeader(me.accessToken))
      .expect(204);

    const res = await request(app)
      .get("/conversations")
      .set(...authHeader(me.accessToken))
      .expect(200);
    const conversation = res.body.find((c: { partner: { user_id: string } }) => c.partner.user_id === partner.userId);
    expect(conversation.unread_count).toBe(0);
    expect(conversation.last_message.read_at).not.toBeNull();
  });

  test("既読化の後に届いたメッセージは未読として数えられる（0固定にしていないこと）", async () => {
    const { me, partner } = await connectedPair();
    await sendMessage(partner, me.userId, "1通目");

    await request(app)
      .post(`/conversations/${partner.userId}/read`)
      .set(...authHeader(me.accessToken))
      .expect(204);

    // 既読化の後に届いた新着
    await sendMessage(partner, me.userId, "2通目");

    const res = await request(app)
      .get("/conversations")
      .set(...authHeader(me.accessToken))
      .expect(200);
    const conversation = res.body.find((c: { partner: { user_id: string } }) => c.partner.user_id === partner.userId);
    expect(conversation.unread_count).toBe(1);
    expect(conversation.last_message.content).toBe("2通目");
    expect(conversation.last_message.read_at).toBeNull();
  });

  test("既読化してもカウンタがマイナスにならない（連続実行しても0のまま）", async () => {
    const { me, partner } = await connectedPair();
    await sendMessage(partner, me.userId, "1通目");

    for (let i = 0; i < 3; i++) {
      await request(app)
        .post(`/conversations/${partner.userId}/read`)
        .set(...authHeader(me.accessToken))
        .expect(204);
    }

    const connectionSnap = await db.collection("connections").doc(buildPairId(me.userId, partner.userId)).get();
    const data = connectionSnap.data()!;
    expect(data.unreadCountForUserA ?? 0).toBe(0);
    expect(data.unreadCountForUserB ?? 0).toBe(0);
  });

  test("ブロックした相手との会話は会話一覧から除外される", async () => {
    const { me, partner } = await connectedPair();
    await sendMessage(partner, me.userId, "こんにちは");

    await request(app)
      .post(`/users/${partner.userId}/block`)
      .set(...authHeader(me.accessToken))
      .expect(204);

    const res = await request(app)
      .get("/conversations")
      .set(...authHeader(me.accessToken))
      .expect(200);
    // 送信すると403になる会話が一覧に残り続けると、閉じる手段が無いまま表示され続けてしまう
    expect(res.body.find((c: { partner: { user_id: string } }) => c.partner.user_id === partner.userId)).toBeUndefined();
  });

  test("他人のプロフィールには is_admin を含めない（管理者アカウントの列挙防止）", async () => {
    const me = await registerNewUser(app);
    const other = await registerNewUser(app);

    const otherRes = await request(app)
      .get(`/users/${other.userId}`)
      .set(...authHeader(me.accessToken))
      .expect(200);
    expect(otherRes.body.is_admin).toBeUndefined();
    // クライアントが利用する他の項目は従来どおり返る
    expect(otherRes.body.status).toBe("ACTIVE");

    const selfRes = await request(app)
      .get(`/users/${me.userId}`)
      .set(...authHeader(me.accessToken))
      .expect(200);
    expect(selfRes.body.is_admin).toBe(false);
  });

  test("パス引数に不正なIDを渡しても500ではなく400を返す", async () => {
    const me = await registerNewUser(app);

    // `%2F`はExpressがパス引数へデコードするため、Firestoreの`doc()`が例外を投げ500になりうる
    const res = await request(app)
      .post("/conversations/a%2Fb/read")
      .set(...authHeader(me.accessToken))
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });
});
