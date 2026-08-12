import request from "supertest";
import { createApp } from "../../src/app";
import { seedArea } from "../helpers/fixtures";

/**
 * `GET /areas`（技術設計書6-2章）。
 * `is_active=true`のみ・`display_order`昇順で返すことを検証する。認証不要。
 */
describe("GET /areas", () => {
  const app = createApp();

  test("エリアが1件も無い場合は空配列を返す", async () => {
    const res = await request(app).get("/areas").expect(200);
    expect(res.body).toEqual([]);
  });

  test("認証ヘッダーが無くても200を返す（認証不要、技術設計書6-2章）", async () => {
    await seedArea({ isActive: true, displayOrder: 1 });
    await request(app).get("/areas").expect(200);
  });

  test("is_active=trueのみを返し、is_active=falseは除外する", async () => {
    const activeId = await seedArea({ areaName: "有効エリア", isActive: true, displayOrder: 1 });
    await seedArea({ areaName: "無効エリア", isActive: false, displayOrder: 2 });

    const res = await request(app).get("/areas").expect(200);
    expect(res.body).toHaveLength(1);
    expect(res.body[0].area_id).toBe(activeId);
    expect(res.body[0].is_active).toBe(true);
  });

  test("display_order昇順でソートされて返る", async () => {
    const third = await seedArea({ areaName: "3番目", isActive: true, displayOrder: 30 });
    const first = await seedArea({ areaName: "1番目", isActive: true, displayOrder: 10 });
    const second = await seedArea({ areaName: "2番目", isActive: true, displayOrder: 20 });

    const res = await request(app).get("/areas").expect(200);
    expect(res.body.map((a: { area_id: string }) => a.area_id)).toEqual([first, second, third]);
  });

  test("レスポンス形式が技術設計書6-2章・AndroidクライアントAreaDtoと一致するフィールド名を持つ", async () => {
    await seedArea({
      prefecture: "東京都",
      areaName: "テストエリア",
      isActive: true,
      displayOrder: 1,
    });
    const res = await request(app).get("/areas").expect(200);
    expect(res.body[0]).toEqual(
      expect.objectContaining({
        area_id: expect.any(String),
        prefecture: "東京都",
        area_name: "テストエリア",
        display_order: 1,
        is_active: true,
        created_at: expect.any(String),
      })
    );
  });
});
