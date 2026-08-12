import request from "supertest";
import { createApp } from "../../src/app";
import { authHeader, registerNewUser, seedArea } from "../helpers/fixtures";

/**
 * `GET /users/{id}` / `PUT /users/{id}`（技術設計書6-3章）。
 * 本人以外403、未認証401、バリデーション（年齢0〜120・スコア40〜200等）を検証する。
 */
describe("GET /users/{id}", () => {
  const app = createApp();

  test("未認証（Authorizationヘッダー無し）は401 UNAUTHENTICATEDを返す", async () => {
    const registered = await registerNewUser(app);
    const res = await request(app).get(`/users/${registered.userId}`).expect(401);
    expect(res.body.error.code).toBe("UNAUTHENTICATED");
  });

  test("無効なアクセストークンは401 UNAUTHENTICATEDを返す", async () => {
    const registered = await registerNewUser(app);
    const res = await request(app)
      .get(`/users/${registered.userId}`)
      .set("Authorization", "Bearer invalid-token")
      .expect(401);
    expect(res.body.error.code).toBe("UNAUTHENTICATED");
  });

  test("認証済みユーザーは自分自身のプロフィールを取得できる", async () => {
    const registered = await registerNewUser(app);
    const res = await request(app)
      .get(`/users/${registered.userId}`)
      .set(...authHeader(registered.accessToken))
      .expect(200);
    expect(res.body.user_id).toBe(registered.userId);
    expect(res.body.area.area_id).toBe(registered.areaId);
  });

  test("認証済みユーザーは他人のプロフィールも取得できる（GET /users/{id}に本人限定の認可は明記されていないため、技術設計書6-3章のとおり閲覧系は制限なしと解釈）", async () => {
    const viewer = await registerNewUser(app);
    const target = await registerNewUser(app);
    const res = await request(app)
      .get(`/users/${target.userId}`)
      .set(...authHeader(viewer.accessToken))
      .expect(200);
    expect(res.body.user_id).toBe(target.userId);
  });

  /**
   * 【修正確認・PII保護】他人のプロフィール取得時は生の電話番号(phone_number)を含めない。
   *
   * PRD（`docs/要件定義書.md`5章「非機能要件」）が「個人情報は第三者に不必要に開示しない設計とする」
   * と明記していること、`GetUserUseCase`（Androidクライアント）が`BoardViewModel`・
   * `MessageThreadViewModel`から「他人」のプロフィール取得にも使われることを踏まえ、`toUserResponse()`
   * を修正し、リクエストしたユーザー本人が対象ユーザー自身の場合のみ`phone_number`を含めるようにした
   * （`docs/test-plan.md` 6-4-2章参照）。`phone_verified`（真偽値）は電話番号自体ではないため、
   * 他人閲覧時も引き続き含める。
   */
  test("他人のプロフィール取得時はphone_number（生の電話番号）が含まれない", async () => {
    const viewer = await registerNewUser(app);
    const target = await registerNewUser(app);
    const res = await request(app)
      .get(`/users/${target.userId}`)
      .set(...authHeader(viewer.accessToken))
      .expect(200);
    expect(res.body.phone_number).toBeUndefined();
    expect(res.body.phone_verified).toBe(true);
  });

  test("本人によるプロフィール取得時はphone_number（生の電話番号）が含まれる", async () => {
    const registered = await registerNewUser(app);
    const res = await request(app)
      .get(`/users/${registered.userId}`)
      .set(...authHeader(registered.accessToken))
      .expect(200);
    expect(res.body.phone_number).toBe(registered.phoneNumber);
  });

  test("存在しないuser_idは404 NOT_FOUNDを返す", async () => {
    const registered = await registerNewUser(app);
    const res = await request(app)
      .get("/users/存在しないユーザーID")
      .set(...authHeader(registered.accessToken))
      .expect(404);
    expect(res.body.error.code).toBe("NOT_FOUND");
  });
});

describe("PUT /users/{id}", () => {
  const app = createApp();

  function validBody(areaId: string, overrides: Record<string, unknown> = {}) {
    return {
      name: "更新後の名前",
      gender: "FEMALE",
      age: 40,
      area_id: areaId,
      average_score: 120,
      purpose: "SERIOUS",
      introduction: "更新後の自己紹介",
      ...overrides,
    };
  }

  test("未認証は401 UNAUTHENTICATEDを返す", async () => {
    const registered = await registerNewUser(app);
    const res = await request(app)
      .put(`/users/${registered.userId}`)
      .send(validBody(registered.areaId))
      .expect(401);
    expect(res.body.error.code).toBe("UNAUTHENTICATED");
  });

  test("本人以外がPUTすると403 FORBIDDENを返す（DeveloperAgentの実装判断、実装メモ参照）", async () => {
    const owner = await registerNewUser(app);
    const other = await registerNewUser(app);
    const res = await request(app)
      .put(`/users/${owner.userId}`)
      .set(...authHeader(other.accessToken))
      .send(validBody(owner.areaId))
      .expect(403);
    expect(res.body.error.code).toBe("FORBIDDEN");
  });

  test("本人によるPUTは200で更新後のプロフィールを返す", async () => {
    const owner = await registerNewUser(app);
    const res = await request(app)
      .put(`/users/${owner.userId}`)
      .set(...authHeader(owner.accessToken))
      .send(validBody(owner.areaId, { name: "山田花子" }))
      .expect(200);
    expect(res.body.name).toBe("山田花子");
    expect(res.body.purpose).toBe("SERIOUS");
    expect(res.body.average_score).toBe(120);

    // 実際にFirestoreへ反映されていることをGETで再確認
    const getRes = await request(app)
      .get(`/users/${owner.userId}`)
      .set(...authHeader(owner.accessToken))
      .expect(200);
    expect(getRes.body.name).toBe("山田花子");
  });

  test.each([
    ["age", -1],
    ["age", 121],
  ])("%sが範囲外(%i)の場合は400 VALIDATION_ERRORを返す", async (_field, age) => {
    const owner = await registerNewUser(app);
    const res = await request(app)
      .put(`/users/${owner.userId}`)
      .set(...authHeader(owner.accessToken))
      .send(validBody(owner.areaId, { age }))
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test.each([
    ["average_score", 39],
    ["average_score", 201],
  ])("%sが範囲外(%i)の場合は400 VALIDATION_ERRORを返す", async (_field, averageScore) => {
    const owner = await registerNewUser(app);
    const res = await request(app)
      .put(`/users/${owner.userId}`)
      .set(...authHeader(owner.accessToken))
      .send(validBody(owner.areaId, { average_score: averageScore }))
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("存在しない・非活性のarea_idを指定すると400 VALIDATION_ERRORを返す", async () => {
    const owner = await registerNewUser(app);
    const inactiveAreaId = await seedArea({ isActive: false });
    const res = await request(app)
      .put(`/users/${owner.userId}`)
      .set(...authHeader(owner.accessToken))
      .send(validBody(inactiveAreaId))
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("不正なpurpose enumは400 VALIDATION_ERRORを返す", async () => {
    const owner = await registerNewUser(app);
    const res = await request(app)
      .put(`/users/${owner.userId}`)
      .set(...authHeader(owner.accessToken))
      .send(validBody(owner.areaId, { purpose: "UNKNOWN_PURPOSE" }))
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });
});
