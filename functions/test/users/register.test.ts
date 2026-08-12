import request from "supertest";
import { createApp } from "../../src/app";
import { extractLatestOtpCode } from "../setup/consoleCapture";
import { nextPhoneNumber, registerNewUser, seedArea } from "../helpers/fixtures";

/**
 * `POST /users`（新規登録。技術設計書6-1章）。
 * registration_tokenの検証、作成後access_token発行を検証する。
 */
describe("POST /users", () => {
  const app = createApp();

  async function verifiedRegistrationToken(phoneNumber: string): Promise<string> {
    await request(app).post("/auth/phone/otp").send({ phone_number: phoneNumber }).expect(204);
    const otp = extractLatestOtpCode(phoneNumber);
    const verifyRes = await request(app)
      .post("/auth/phone/verify")
      .send({ phone_number: phoneNumber, otp_code: otp })
      .expect(200);
    return verifyRes.body.registration_token as string;
  }

  test("有効なregistration_tokenで登録すると201、作成されたuserとaccess_tokenが返る", async () => {
    const phoneNumber = nextPhoneNumber();
    const areaId = await seedArea();
    const token = await verifiedRegistrationToken(phoneNumber);

    const res = await request(app)
      .post("/users")
      .send({
        registration_token: token,
        name: "山田太郎",
        gender: "MALE",
        age: 28,
        area_id: areaId,
        average_score: 90,
        purpose: "CASUAL",
        introduction: "よろしくお願いします",
      })
      .expect(201);

    expect(res.body.user.name).toBe("山田太郎");
    expect(res.body.user.area.area_id).toBe(areaId);
    expect(res.body.user.phone_verified).toBe(true);
    expect(res.body.user.is_admin).toBe(false);
    expect(typeof res.body.access_token).toBe("string");
    expect(res.body.access_token.length).toBeGreaterThan(0);
  });

  test("access_token発行後、そのトークンで認証必須APIを呼び出せる", async () => {
    const registered = await registerNewUser(app);
    const res = await request(app)
      .get(`/users/${registered.userId}`)
      .set("Authorization", `Bearer ${registered.accessToken}`)
      .expect(200);
    expect(res.body.user_id).toBe(registered.userId);
  });

  test("不正なregistration_tokenは401 UNAUTHENTICATEDを返す", async () => {
    const areaId = await seedArea();
    const res = await request(app)
      .post("/users")
      .send({
        registration_token: "invalid-token-xxxxx",
        name: "テスト",
        gender: "MALE",
        age: 20,
        area_id: areaId,
        average_score: 100,
        purpose: "CASUAL",
        introduction: "",
      })
      .expect(401);
    expect(res.body.error.code).toBe("UNAUTHENTICATED");
  });

  test("registration_tokenは使い切り（1回限り）であり、2回目の登録は401を返す", async () => {
    const phoneNumber = nextPhoneNumber();
    const areaId = await seedArea();
    const token = await verifiedRegistrationToken(phoneNumber);
    const body = {
      registration_token: token,
      name: "初回登録",
      gender: "MALE",
      age: 25,
      area_id: areaId,
      average_score: 80,
      purpose: "CASUAL",
      introduction: "",
    };
    await request(app).post("/users").send(body).expect(201);

    const res = await request(app)
      .post("/users")
      .send({ ...body, name: "2回目登録" })
      .expect(401);
    expect(res.body.error.code).toBe("UNAUTHENTICATED");
  });

  test("registration_tokenが空文字の場合は400 VALIDATION_ERRORを返す（zodバリデーション）", async () => {
    const areaId = await seedArea();
    const res = await request(app)
      .post("/users")
      .send({
        registration_token: "",
        name: "テスト",
        gender: "MALE",
        age: 20,
        area_id: areaId,
        average_score: 100,
        purpose: "CASUAL",
        introduction: "",
      })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("存在しない/無効なarea_idは400 VALIDATION_ERRORを返す", async () => {
    const phoneNumber = nextPhoneNumber();
    const token = await verifiedRegistrationToken(phoneNumber);
    const res = await request(app)
      .post("/users")
      .send({
        registration_token: token,
        name: "テスト",
        gender: "MALE",
        age: 20,
        area_id: "存在しないID",
        average_score: 100,
        purpose: "CASUAL",
        introduction: "",
      })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("is_active=falseのarea_idは選択できず400 VALIDATION_ERRORを返す", async () => {
    const inactiveAreaId = await seedArea({ isActive: false });
    const phoneNumber = nextPhoneNumber();
    const token = await verifiedRegistrationToken(phoneNumber);
    const res = await request(app)
      .post("/users")
      .send({
        registration_token: token,
        name: "テスト",
        gender: "MALE",
        age: 20,
        area_id: inactiveAreaId,
        average_score: 100,
        purpose: "CASUAL",
        introduction: "",
      })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test.each([
    ["age", -1],
    ["age", 121],
  ])("%sが範囲外(%i)の場合は400 VALIDATION_ERRORを返す", async (_field, age) => {
    const phoneNumber = nextPhoneNumber();
    const areaId = await seedArea();
    const token = await verifiedRegistrationToken(phoneNumber);
    const res = await request(app)
      .post("/users")
      .send({
        registration_token: token,
        name: "テスト",
        gender: "MALE",
        age,
        area_id: areaId,
        average_score: 100,
        purpose: "CASUAL",
        introduction: "",
      })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test.each([
    ["average_score", 39],
    ["average_score", 201],
  ])("%sが範囲外(%i)の場合は400 VALIDATION_ERRORを返す", async (_field, averageScore) => {
    const phoneNumber = nextPhoneNumber();
    const areaId = await seedArea();
    const token = await verifiedRegistrationToken(phoneNumber);
    const res = await request(app)
      .post("/users")
      .send({
        registration_token: token,
        name: "テスト",
        gender: "MALE",
        age: 30,
        area_id: areaId,
        average_score: averageScore,
        purpose: "CASUAL",
        introduction: "",
      })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("age=0・age=120（境界値）は許容される", async () => {
    for (const age of [0, 120]) {
      const phoneNumber = nextPhoneNumber();
      const areaId = await seedArea();
      const token = await verifiedRegistrationToken(phoneNumber);
      await request(app)
        .post("/users")
        .send({
          registration_token: token,
          name: "境界値テスト",
          gender: "MALE",
          age,
          area_id: areaId,
          average_score: 100,
          purpose: "CASUAL",
          introduction: "",
        })
        .expect(201);
    }
  });

  test("average_score=40・average_score=200（境界値）は許容される", async () => {
    for (const averageScore of [40, 200]) {
      const phoneNumber = nextPhoneNumber();
      const areaId = await seedArea();
      const token = await verifiedRegistrationToken(phoneNumber);
      await request(app)
        .post("/users")
        .send({
          registration_token: token,
          name: "境界値テスト",
          gender: "MALE",
          age: 30,
          area_id: areaId,
          average_score: averageScore,
          purpose: "CASUAL",
          introduction: "",
        })
        .expect(201);
    }
  });
});
