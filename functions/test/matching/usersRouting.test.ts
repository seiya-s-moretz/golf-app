import request from "supertest";
import { createApp } from "../../src/app";
import { extractLatestOtpCode } from "../setup/consoleCapture";
import { authHeader, nextPhoneNumber, registerNewUser, seedArea } from "../helpers/fixtures";

/**
 * `usersMatchingRoutes`（`GET /users/recommend`等）と`usersRoutes`（Phase1、変更禁止）が
 * 同じ`/users`プレフィックスを共有することによるルーティングの回帰確認（`matching.routes.ts`実装メモ参照）。
 *
 * `usersMatchingRoutes`はルーター単位(`router.use(authenticate)`)ではなくルート単位で`authenticate`を
 * 付与している。これは、ルーター単位で付与すると「このルーター内のどのパスにもマッチしないリクエスト」
 * （例: 認証不要な`POST /users`新規登録）にも認証チェックが先に走ってしまい401になる不具合を
 * DeveloperAgentが実装中に検出・修正した結果である。本テストはその修正が有効であることを確認する。
 */
describe("【回帰確認】usersMatchingRoutesとusersRoutesの/usersプレフィックス共有", () => {
  const app = createApp();

  test("POST /users(新規登録)は未認証のままでも401にならず201で成功する", async () => {
    const phoneNumber = nextPhoneNumber();
    const areaId = await seedArea();
    await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(204);
    const otpCode = extractLatestOtpCode(phoneNumber);
    const verifyRes = await request(app)
      .post("/auth/phone/verify")
      .send({ phone_number: phoneNumber, otp_code: otpCode })
      .expect(200);

    const res = await request(app)
      .post("/users")
      .send({
        registration_token: verifyRes.body.registration_token,
        name: "ルーティング確認太郎",
        gender: "MALE",
        age: 30,
        area_id: areaId,
        average_score: 100,
        purpose: "CASUAL",
        introduction: "よろしくお願いします",
      })
      .expect(201);
    expect(res.body.user.user_id).toBeDefined();
    expect(res.body.access_token).toBeDefined();
  });

  test("GET /users/{id}はusersMatchingRoutesに飲み込まれずusersRoutesまでフォールスルーして200を返す", async () => {
    const viewer = await registerNewUser(app);
    const target = await registerNewUser(app);
    const res = await request(app)
      .get(`/users/${target.userId}`)
      .set(...authHeader(viewer.accessToken))
      .expect(200);
    expect(res.body.user_id).toBe(target.userId);
  });

  test("PUT /users/{id}はusersMatchingRoutesに飲み込まれずusersRoutesまでフォールスルーして200を返す", async () => {
    const owner = await registerNewUser(app);
    const res = await request(app)
      .put(`/users/${owner.userId}`)
      .set(...authHeader(owner.accessToken))
      .send({
        name: "更新後の名前",
        gender: "FEMALE",
        age: 40,
        area_id: owner.areaId,
        average_score: 120,
        purpose: "SERIOUS",
        introduction: "更新後の自己紹介",
      })
      .expect(200);
    expect(res.body.name).toBe("更新後の名前");
  });

  test("GET /users/recommendは未認証だと401(usersRoutesにフォールスルーせずusersMatchingRoutes自身で認証チェックされる)", async () => {
    const res = await request(app).get("/users/recommend").expect(401);
    expect(res.body.error.code).toBe("UNAUTHENTICATED");
  });

  test("GET /users/me/match-requestsが誤ってGET /users/:id(id='me')として解釈されない(usersMatchingRoutesが先にマウントされているため)", async () => {
    const me = await registerNewUser(app);
    const res = await request(app)
      .get("/users/me/match-requests?direction=received")
      .set(...authHeader(me.accessToken))
      .expect(200);
    // usersRoutesのGET /:idに誤って一致していた場合、ユーザーID="me"が見つからず404になるはず。
    // 期待通りusersMatchingRoutesで処理されれば200かつ配列が返る。
    expect(Array.isArray(res.body)).toBe(true);
  });
});
