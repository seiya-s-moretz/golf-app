import request from "supertest";
import { createApp } from "../../src/app";
import { authHeader, registerNewUser } from "../helpers/fixtures";

/**
 * `POST /round-events`の入力バリデーション（技術設計書6-4章）。
 *
 * とくに`datetime`は、Androidクライアントが`kotlinx.datetime.Instant.parse()`で解釈するため
 * **オフセット付きISO-8601以外を保存させてはならない**。`GET /round-events`は一覧を一括変換するので、
 * 1件でも壊れた形式が保存されると一覧全体が全ユーザーで表示できなくなる。
 */
describe("POST /round-events のバリデーション", () => {
  const app = createApp();

  const validBody = {
    club_name: "テストゴルフ倶楽部",
    datetime: "2026-09-01T09:00:00+09:00",
    fee: 10000,
    capacity: 4,
  };

  // `async`にすると戻り値がPromiseになりsupertestの`.expect()`を繋げられなくなるため同期関数にする
  function post(accessToken: string, overrides: Record<string, unknown>) {
    return request(app)
      .post("/round-events")
      .set(...authHeader(accessToken))
      .send({ ...validBody, ...overrides });
  }

  test.each([
    ["オフセット付きISO-8601", "2026-09-01T09:00:00+09:00"],
    ["UTC(Z)表記", "2026-09-01T00:00:00Z"],
    ["秒とミリ秒を含む表記", "2026-09-01T00:00:00.500Z"],
    ["秒を省いた表記", "2026-09-01T09:00+09:00"],
  ])("datetimeが%sなら受理される", async (_label, datetime) => {
    const user = await registerNewUser(app);
    const res = await post(user.accessToken, { datetime }).expect(201);
    expect(res.body.datetime).toBe(datetime);
  });

  test.each([
    ["スペース区切り", "2026-09-01 09:00"],
    ["スラッシュ区切り", "2026/09/01"],
    ["英語表記", "Sep 1 2026"],
    ["オフセットなし", "2026-09-01T09:00:00"],
    ["日付のみ", "2026-09-01"],
    ["空文字", ""],
  ])("datetimeが%sなら400 VALIDATION_ERRORで弾く（Androidのパースが落ちるため）", async (_label, datetime) => {
    const user = await registerNewUser(app);
    const res = await post(user.accessToken, { datetime }).expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("capacityが上限を超えると400を返す", async () => {
    const user = await registerNewUser(app);
    const res = await post(user.accessToken, { capacity: 101 }).expect(400);
    expect(res.body.error.code).toBe("VALIDATION_ERROR");
  });

  test("feeが上限を超えると400を返す", async () => {
    const user = await registerNewUser(app);
    await post(user.accessToken, { fee: 1_000_001 }).expect(400);
  });

  test("club_nameが長すぎると400を返す", async () => {
    const user = await registerNewUser(app);
    await post(user.accessToken, { club_name: "あ".repeat(101) }).expect(400);
  });
});
