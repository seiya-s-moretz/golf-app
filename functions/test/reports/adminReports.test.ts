import request from "supertest";
import { createApp } from "../../src/app";
import { authHeader, registerNewUser, setAdmin } from "../helpers/fixtures";

/**
 * `GET /admin/reports`・`GET /admin/reports/{id}`・`PATCH /admin/reports/{id}/status`
 * （技術設計書6-9章、ADR-0007）。
 */
describe("通報管理(admin)API", () => {
  const app = createApp();

  async function createReport(
    reporterToken: string,
    targetType: "USER" | "BOARD_POST",
    targetId: string,
    reasonCategory = "SPAM"
  ) {
    return request(app)
      .post("/reports")
      .set(...authHeader(reporterToken))
      .send({ target_type: targetType, target_id: targetId, reason_category: reasonCategory })
      .expect(201);
  }

  describe("認可(is_admin)", () => {
    test("GET /admin/reportsは未認証だと401 UNAUTHENTICATEDを返す", async () => {
      const res = await request(app).get("/admin/reports").expect(401);
      expect(res.body.error.code).toBe("UNAUTHENTICATED");
    });

    test("GET /admin/reportsはis_admin=falseだと403 FORBIDDENを返す", async () => {
      const user = await registerNewUser(app);
      const res = await request(app)
        .get("/admin/reports")
        .set(...authHeader(user.accessToken))
        .expect(403);
      expect(res.body.error.code).toBe("FORBIDDEN");
    });

    test("GET /admin/reports/{id}はis_admin=falseだと403 FORBIDDENを返す", async () => {
      const user = await registerNewUser(app);
      const res = await request(app)
        .get("/admin/reports/dummy-id")
        .set(...authHeader(user.accessToken))
        .expect(403);
      expect(res.body.error.code).toBe("FORBIDDEN");
    });

    test("PATCH /admin/reports/{id}/statusはis_admin=falseだと403 FORBIDDENを返す", async () => {
      const user = await registerNewUser(app);
      const res = await request(app)
        .patch("/admin/reports/dummy-id/status")
        .set(...authHeader(user.accessToken))
        .send({ status: "REVIEWING" })
        .expect(403);
      expect(res.body.error.code).toBe("FORBIDDEN");
    });
  });

  describe("GET /admin/reports（一覧・絞り込み・ページネーション）", () => {
    test("is_admin=trueなら全件をcreated_at降順で返す", async () => {
      const admin = await registerNewUser(app);
      await setAdmin(admin.userId);
      const reporter = await registerNewUser(app);
      const target1 = await registerNewUser(app);
      const target2 = await registerNewUser(app);

      const r1 = await createReport(reporter.accessToken, "USER", target1.userId);
      const r2 = await createReport(reporter.accessToken, "USER", target2.userId);

      const res = await request(app)
        .get("/admin/reports")
        .set(...authHeader(admin.accessToken))
        .expect(200);
      expect(res.body).toHaveLength(2);
      expect(res.body[0].report_id).toBe(r2.body.report_id);
      expect(res.body[1].report_id).toBe(r1.body.report_id);
      expect(res.body[0].reporter.user_id).toBe(reporter.userId);
      expect(res.body[0].target_summary).toBeDefined();
    });

    test("statusで絞り込める", async () => {
      const admin = await registerNewUser(app);
      await setAdmin(admin.userId);
      const reporter = await registerNewUser(app);
      const target1 = await registerNewUser(app);
      const target2 = await registerNewUser(app);
      const r1 = await createReport(reporter.accessToken, "USER", target1.userId);
      await createReport(reporter.accessToken, "USER", target2.userId);

      await request(app)
        .patch(`/admin/reports/${r1.body.report_id}/status`)
        .set(...authHeader(admin.accessToken))
        .send({ status: "RESOLVED" })
        .expect(200);

      const pendingOnly = await request(app)
        .get("/admin/reports?status=PENDING")
        .set(...authHeader(admin.accessToken))
        .expect(200);
      expect(pendingOnly.body).toHaveLength(1);
      expect(pendingOnly.body[0].status).toBe("PENDING");

      const resolvedOnly = await request(app)
        .get("/admin/reports?status=RESOLVED")
        .set(...authHeader(admin.accessToken))
        .expect(200);
      expect(resolvedOnly.body).toHaveLength(1);
      expect(resolvedOnly.body[0].report_id).toBe(r1.body.report_id);
    });

    test("不正なstatus値は400 VALIDATION_ERRORを返す", async () => {
      const admin = await registerNewUser(app);
      await setAdmin(admin.userId);
      const res = await request(app)
        .get("/admin/reports?status=INVALID")
        .set(...authHeader(admin.accessToken))
        .expect(400);
      expect(res.body.error.code).toBe("VALIDATION_ERROR");
    });

    test("limitを指定すると件数が絞り込まれ、beforeカーソルで続きを取得できる", async () => {
      const admin = await registerNewUser(app);
      await setAdmin(admin.userId);
      const reporter = await registerNewUser(app);
      const targets = await Promise.all([registerNewUser(app), registerNewUser(app), registerNewUser(app)]);
      const created: Array<{ body: Record<string, unknown> }> = [];
      // eslint-disable-next-line no-restricted-syntax
      for (const t of targets) {
        // eslint-disable-next-line no-await-in-loop
        created.push(await createReport(reporter.accessToken, "USER", t.userId));
      }

      const firstPage = await request(app)
        .get("/admin/reports?limit=2")
        .set(...authHeader(admin.accessToken))
        .expect(200);
      expect(firstPage.body).toHaveLength(2);

      // カーソルは(created_at, ID)の組で渡す。時刻だけだと同時刻の通報が飛ばされる
      const cursor = firstPage.body[1].created_at as string;
      const cursorId = firstPage.body[1].report_id as string;
      const secondPage = await request(app)
        .get(`/admin/reports?limit=2&before=${encodeURIComponent(cursor)}&before_id=${cursorId}`)
        .set(...authHeader(admin.accessToken))
        .expect(200);
      expect(secondPage.body).toHaveLength(1);

      const allIds = [...firstPage.body, ...secondPage.body].map((r: { report_id: string }) => r.report_id).sort();
      const expectedIds = created.map((c) => c.body.report_id as string).sort();
      expect(allIds).toEqual(expectedIds);
    });

    test("通報が0件の場合は空配列を返す", async () => {
      const admin = await registerNewUser(app);
      await setAdmin(admin.userId);
      const res = await request(app)
        .get("/admin/reports")
        .set(...authHeader(admin.accessToken))
        .expect(200);
      expect(res.body).toEqual([]);
    });
  });

  describe("GET /admin/reports/{id}（詳細）", () => {
    test("USER対象の詳細にはtarget_detail.userが含まれ、phone_numberは含まれない", async () => {
      const admin = await registerNewUser(app);
      await setAdmin(admin.userId);
      const reporter = await registerNewUser(app);
      const target = await registerNewUser(app);
      const created = await createReport(reporter.accessToken, "USER", target.userId, "HARASSMENT");

      const res = await request(app)
        .get(`/admin/reports/${created.body.report_id}`)
        .set(...authHeader(admin.accessToken))
        .expect(200);

      expect(res.body.target_detail.board_post).toBeNull();
      expect(res.body.target_detail.user).not.toBeNull();
      expect(res.body.target_detail.user.user_id).toBe(target.userId);
      expect(res.body.target_detail.user).not.toHaveProperty("phone_number");
      expect(JSON.stringify(res.body.target_detail.user)).not.toContain("phone_number");
      expect(res.body.reporter.user_id).toBe(reporter.userId);
    });

    test("BOARD_POST対象の詳細にはtarget_detail.board_postが含まれ、userはnull", async () => {
      const admin = await registerNewUser(app);
      await setAdmin(admin.userId);
      const reporter = await registerNewUser(app);
      const author = await registerNewUser(app);
      const post = await request(app)
        .post("/board")
        .set(...authHeader(author.accessToken))
        .send({ content: "問題のある投稿本文" })
        .expect(201);
      const created = await createReport(reporter.accessToken, "BOARD_POST", post.body.post_id);

      const res = await request(app)
        .get(`/admin/reports/${created.body.report_id}`)
        .set(...authHeader(admin.accessToken))
        .expect(200);

      expect(res.body.target_detail.user).toBeNull();
      expect(res.body.target_detail.board_post).not.toBeNull();
      expect(res.body.target_detail.board_post.post_id).toBe(post.body.post_id);
      expect(res.body.target_detail.board_post.content).toBe("問題のある投稿本文");
      expect(res.body.target_detail.board_post.user_id).toBe(author.userId);
    });

    test("存在しないIDは404 NOT_FOUNDを返す", async () => {
      const admin = await registerNewUser(app);
      await setAdmin(admin.userId);
      const res = await request(app)
        .get("/admin/reports/存在しない通報ID")
        .set(...authHeader(admin.accessToken))
        .expect(404);
      expect(res.body.error.code).toBe("NOT_FOUND");
    });
  });

  describe("PATCH /admin/reports/{id}/status（ステータス更新）", () => {
    test("ステータス更新でhandled_by_user_id・handled_atが反映される", async () => {
      const admin = await registerNewUser(app);
      await setAdmin(admin.userId);
      const reporter = await registerNewUser(app);
      const target = await registerNewUser(app);
      const created = await createReport(reporter.accessToken, "USER", target.userId);
      expect(created.body.handled_by_user_id).toBeNull();

      const res = await request(app)
        .patch(`/admin/reports/${created.body.report_id}/status`)
        .set(...authHeader(admin.accessToken))
        .send({ status: "REVIEWING", handling_memo: "対応中です" })
        .expect(200);

      expect(res.body.status).toBe("REVIEWING");
      expect(res.body.handled_by_user_id).toBe(admin.userId);
      expect(res.body.handled_at).not.toBeNull();
      expect(res.body.handling_memo).toBe("対応中です");
    });

    test("handling_memoを指定しない更新はhandling_memoを変更しない", async () => {
      const admin = await registerNewUser(app);
      await setAdmin(admin.userId);
      const reporter = await registerNewUser(app);
      const target = await registerNewUser(app);
      const created = await createReport(reporter.accessToken, "USER", target.userId);

      await request(app)
        .patch(`/admin/reports/${created.body.report_id}/status`)
        .set(...authHeader(admin.accessToken))
        .send({ status: "REVIEWING", handling_memo: "最初のメモ" })
        .expect(200);

      const res = await request(app)
        .patch(`/admin/reports/${created.body.report_id}/status`)
        .set(...authHeader(admin.accessToken))
        .send({ status: "RESOLVED" })
        .expect(200);

      expect(res.body.status).toBe("RESOLVED");
      expect(res.body.handling_memo).toBe("最初のメモ");
    });

    test("不正なstatus値は400 VALIDATION_ERRORを返す", async () => {
      const admin = await registerNewUser(app);
      await setAdmin(admin.userId);
      const reporter = await registerNewUser(app);
      const target = await registerNewUser(app);
      const created = await createReport(reporter.accessToken, "USER", target.userId);

      const res = await request(app)
        .patch(`/admin/reports/${created.body.report_id}/status`)
        .set(...authHeader(admin.accessToken))
        .send({ status: "INVALID" })
        .expect(400);
      expect(res.body.error.code).toBe("VALIDATION_ERROR");
    });

    test("状態遷移順序は強制されない(RESOLVEDからPENDINGへの逆行も許可、ADR-0007)", async () => {
      const admin = await registerNewUser(app);
      await setAdmin(admin.userId);
      const reporter = await registerNewUser(app);
      const target = await registerNewUser(app);
      const created = await createReport(reporter.accessToken, "USER", target.userId);

      await request(app)
        .patch(`/admin/reports/${created.body.report_id}/status`)
        .set(...authHeader(admin.accessToken))
        .send({ status: "RESOLVED" })
        .expect(200);

      const res = await request(app)
        .patch(`/admin/reports/${created.body.report_id}/status`)
        .set(...authHeader(admin.accessToken))
        .send({ status: "PENDING" })
        .expect(200);
      expect(res.body.status).toBe("PENDING");
    });

    test("存在しないIDへの更新は404 NOT_FOUNDを返す", async () => {
      const admin = await registerNewUser(app);
      await setAdmin(admin.userId);
      const res = await request(app)
        .patch("/admin/reports/存在しない通報ID/status")
        .set(...authHeader(admin.accessToken))
        .send({ status: "REVIEWING" })
        .expect(404);
      expect(res.body.error.code).toBe("NOT_FOUND");
    });
  });
});
