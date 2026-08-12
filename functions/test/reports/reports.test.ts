import request from "supertest";
import { createApp } from "../../src/app";
import { authHeader, registerNewUser } from "../helpers/fixtures";

/** `POST /reports`（技術設計書6-8章・5-2章）。 */
describe("POST /reports", () => {
  const app = createApp();

  test("未認証は401 UNAUTHENTICATEDを返す", async () => {
    const res = await request(app)
      .post("/reports")
      .send({ target_type: "USER", target_id: "dummy", reason_category: "SPAM" })
      .expect(401);
    expect(res.body.error.code).toBe("UNAUTHENTICATED");
  });

  test("USER対象の通報を作成すると201でstatus=PENDINGを返す", async () => {
    const reporter = await registerNewUser(app);
    const target = await registerNewUser(app);
    const res = await request(app)
      .post("/reports")
      .set(...authHeader(reporter.accessToken))
      .send({ target_type: "USER", target_id: target.userId, reason_category: "HARASSMENT" })
      .expect(201);

    expect(res.body.report_id).toBeDefined();
    expect(res.body.reporter_user_id).toBe(reporter.userId);
    expect(res.body.target_type).toBe("USER");
    expect(res.body.target_id).toBe(target.userId);
    expect(res.body.reason_category).toBe("HARASSMENT");
    expect(res.body.reason_text).toBeNull();
    expect(res.body.status).toBe("PENDING");
    expect(res.body.handled_by_user_id).toBeNull();
    expect(res.body.handled_at).toBeNull();
    expect(res.body.handling_memo).toBeNull();
  });

  test("BOARD_POST対象の通報を作成できる", async () => {
    const reporter = await registerNewUser(app);
    const author = await registerNewUser(app);
    const post = await request(app)
      .post("/board")
      .set(...authHeader(author.accessToken))
      .send({ content: "通報される投稿" })
      .expect(201);

    const res = await request(app)
      .post("/reports")
      .set(...authHeader(reporter.accessToken))
      .send({ target_type: "BOARD_POST", target_id: post.body.post_id, reason_category: "SPAM" })
      .expect(201);
    expect(res.body.target_type).toBe("BOARD_POST");
    expect(res.body.target_id).toBe(post.body.post_id);
  });

  test("reason_category=OTHERでreason_textが未指定だと400 VALIDATION_ERRORを返す", async () => {
    const reporter = await registerNewUser(app);
    const target = await registerNewUser(app);
    const res = await request(app)
      .post("/reports")
      .set(...authHeader(reporter.accessToken))
      .send({ target_type: "USER", target_id: target.userId, reason_category: "OTHER" })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("reason_category=OTHERでreason_textが空白のみだと400 VALIDATION_ERRORを返す", async () => {
    const reporter = await registerNewUser(app);
    const target = await registerNewUser(app);
    const res = await request(app)
      .post("/reports")
      .set(...authHeader(reporter.accessToken))
      .send({ target_type: "USER", target_id: target.userId, reason_category: "OTHER", reason_text: "   " })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("reason_category=OTHERでreason_textを指定すると201で作成できる", async () => {
    const reporter = await registerNewUser(app);
    const target = await registerNewUser(app);
    const res = await request(app)
      .post("/reports")
      .set(...authHeader(reporter.accessToken))
      .send({ target_type: "USER", target_id: target.userId, reason_category: "OTHER", reason_text: "その他の理由" })
      .expect(201);
    expect(res.body.reason_category).toBe("OTHER");
    expect(res.body.reason_text).toBe("その他の理由");
  });

  test("reason_category=OTHER以外ではreason_text未指定でも201で作成できる", async () => {
    const reporter = await registerNewUser(app);
    const target = await registerNewUser(app);
    const res = await request(app)
      .post("/reports")
      .set(...authHeader(reporter.accessToken))
      .send({ target_type: "USER", target_id: target.userId, reason_category: "DATING_SOLICITATION" })
      .expect(201);
    expect(res.body.reason_text).toBeNull();
  });

  test("不正なtarget_typeは400 VALIDATION_ERRORを返す", async () => {
    const reporter = await registerNewUser(app);
    const res = await request(app)
      .post("/reports")
      .set(...authHeader(reporter.accessToken))
      .send({ target_type: "INVALID", target_id: "dummy", reason_category: "SPAM" })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("不正なreason_categoryは400 VALIDATION_ERRORを返す", async () => {
    const reporter = await registerNewUser(app);
    const target = await registerNewUser(app);
    const res = await request(app)
      .post("/reports")
      .set(...authHeader(reporter.accessToken))
      .send({ target_type: "USER", target_id: target.userId, reason_category: "INVALID" })
      .expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("存在しない通報対象(USER)は404 NOT_FOUNDを返す", async () => {
    const reporter = await registerNewUser(app);
    const res = await request(app)
      .post("/reports")
      .set(...authHeader(reporter.accessToken))
      .send({ target_type: "USER", target_id: "存在しないユーザーID", reason_category: "SPAM" })
      .expect(404);
    expect(res.body.error.code).toBe("NOT_FOUND");
  });

  test("存在しない通報対象(BOARD_POST)は404 NOT_FOUNDを返す", async () => {
    const reporter = await registerNewUser(app);
    const res = await request(app)
      .post("/reports")
      .set(...authHeader(reporter.accessToken))
      .send({ target_type: "BOARD_POST", target_id: "存在しない投稿ID", reason_category: "SPAM" })
      .expect(404);
    expect(res.body.error.code).toBe("NOT_FOUND");
  });
});
