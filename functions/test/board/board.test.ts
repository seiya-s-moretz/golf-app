import { Timestamp } from "firebase-admin/firestore";
import request from "supertest";
import { createApp } from "../../src/app";
import { db } from "../../src/config/firebaseAdmin";
import { authHeader, registerNewUser } from "../helpers/fixtures";

/**
 * `GET /board`・`POST /board`（技術設計書6-6章）。
 * `GET /board`は`before`/`before_id`/`limit`によるカーソル型ページネーションに対応する（2026-08-13追加）。
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

  test("GET /boardは全ユーザーの投稿を作成日時降順で返す", async () => {
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

  test("limitで件数を絞り込み、beforeカーソルで続きを取得できる", async () => {
    const user = await registerNewUser(app);
    for (const content of ["1件目", "2件目", "3件目", "4件目"]) {
      await request(app)
        .post("/board")
        .set(...authHeader(user.accessToken))
        .send({ content })
        .expect(201);
    }

    const firstPage = await request(app)
      .get("/board?limit=2")
      .set(...authHeader(user.accessToken))
      .expect(200);
    expect(firstPage.body).toHaveLength(2);
    expect(firstPage.body[0].content).toBe("4件目");
    expect(firstPage.body[1].content).toBe("3件目");

    const cursor = firstPage.body[1].created_at as string;
    const cursorId = firstPage.body[1].post_id as string;
    const secondPage = await request(app)
      .get(`/board?limit=2&before=${encodeURIComponent(cursor)}&before_id=${cursorId}`)
      .set(...authHeader(user.accessToken))
      .expect(200);
    expect(secondPage.body).toHaveLength(2);
    expect(secondPage.body[0].content).toBe("2件目");
    expect(secondPage.body[1].content).toBe("1件目");
  });

  test("created_atが完全に同一の投稿でもページ境界で取りこぼさない", async () => {
    const user = await registerNewUser(app);
    // 同一ミリ秒に複数件作成される状況（同時投稿・一括投入・データ移行など）を再現する。
    // 時刻だけのカーソル（created_at < cursor）だと、境界にある同時刻の投稿が
    // どのページにも現れず永久に読めなくなる
    const sameTime = Timestamp.fromMillis(1_800_000_000_000);
    const postIds = ["post-tie-1", "post-tie-2", "post-tie-3"];
    for (const postId of postIds) {
      // eslint-disable-next-line no-await-in-loop
      await db.collection("boardPosts").doc(postId).set({
        postId,
        userId: user.userId,
        content: `同時刻投稿 ${postId}`,
        createdAt: sameTime,
      });
    }

    const collected: string[] = [];
    let before: string | undefined;
    let beforeId: string | undefined;
    for (let page = 0; page < 5; page++) {
      const query = before
        ? `/board?limit=2&before=${encodeURIComponent(before)}&before_id=${beforeId}`
        : "/board?limit=2";
      // eslint-disable-next-line no-await-in-loop
      const res = await request(app)
        .get(query)
        .set(...authHeader(user.accessToken))
        .expect(200);
      if (res.body.length === 0) break;
      collected.push(...res.body.map((p: { post_id: string }) => p.post_id));
      before = res.body[res.body.length - 1].created_at as string;
      beforeId = res.body[res.body.length - 1].post_id as string;
    }

    expect(collected.sort()).toEqual([...postIds].sort());
  });

  test("不正なbefore・limitは黙って無視せず400を返す", async () => {
    const user = await registerNewUser(app);

    // 無視すると1ページ目が返り続け、ページングするクライアントが無限ループする
    const badBefore = await request(app)
      .get("/board?before=garbage&before_id=dummy")
      .set(...authHeader(user.accessToken))
      .expect(400);
    expect(badBefore.body.error.code).toBe("VALIDATION_ERROR");

    await request(app)
      .get("/board?limit=abc")
      .set(...authHeader(user.accessToken))
      .expect(400);

    // 片方だけの指定も曖昧なため400（beforeだけだと同時刻の取りこぼしが起きる）
    await request(app)
      .get("/board?before=2026-08-13T00:00:00.000Z")
      .set(...authHeader(user.accessToken))
      .expect(400);
  });

  test("contentが長すぎる投稿は400 VALIDATION_ERRORを返す", async () => {
    const user = await registerNewUser(app);
    const res = await request(app)
      .post("/board")
      .set(...authHeader(user.accessToken))
      .send({ content: "あ".repeat(1001) })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });
});
