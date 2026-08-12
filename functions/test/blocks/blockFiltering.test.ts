import request from "supertest";
import { createApp } from "../../src/app";
import { authHeader, registerNewUser, seedArea } from "../helpers/fixtures";

/**
 * ブロックによる遡及フィルタ（`GET /round-events`・`GET /users/recommend`・`GET /board`からの除外、
 * `POST /users/{id}/match-requests`・`POST /round-events/{id}/join-requests`のブロック時拒否）。
 *
 * 【重要・設計確認事項】DeveloperAgentは上記3つの一覧系APIすべてで「双方向ブロック」
 * （自分がブロックした relatedか、相手からブロックされているか、いずれか）による除外を実装している。
 * 技術設計書5-2章 Block「効果」の一覧（プロダクトオーナー確認済み、10章#1参照）は以下のとおり書き分けている:
 *   - `GET /users/recommend`: 「ブロック関係（双方向）にあるユーザーを結果から除外」→ 双方向と明記
 *   - `GET /round-events`: 「ブロック関係にある募集作成者の募集を結果から除外」→ 「関係にある」という
 *     表現で双方向寄りだが「双方向」の明記は無い
 *   - `GET /board`: 「ブロックしたユーザーの投稿を除外」→ 「ブロックした」という能動表現であり、
 *     recommendとは異なり「双方向」と明記されていない。文言単独で読む限り、
 *     自分がブロックした相手の投稿のみを除外する片方向の実装を示唆しているようにも読める
 * 一方、技術設計書12-2-3章（実装レベルの補足設計）は「一覧系API（GET /round-events, GET /users/recommend,
 * GET /board）のブロック除外フィルタは...`blocker_user_id==me`・`blocked_user_id==me`の2クエリで
 * 取得しUnionを取る」と明記しており、3つのAPIを区別せず同一の双方向ロジックを共通適用する設計が
 * 書かれている（`excludeBlockedUsers`関数として12-4章でも共通化が明記されている）。
 *
 * TesterAgentの判断: 5-2章の文言（特にboardの「ブロックした」という表現）と12-2-3章の実装レベルの
 * 記述には字面上の緊張関係があるが、(a) 12-2-3章は5-2章より後段の実装設計であり、3APIへの適用を
 * 明示的に一般化して记述していること、(b) 効果一覧の他項目（マッチング申請・ラウンド参加申請・
 * メッセージ送信の拒否）は「ブロック関係にあるユーザー間」という双方向の表現で統一されており、
 * boardのみ意図的に非対称にする合理的理由が5-2章の文言からは読み取れないこと、(c) 安全機能としての
 * ブロックは「自分をブロックした相手の存在も見せない」という双方向の対称設計のほうが一般的なブロック
 * 機能の直感に合致すること、を踏まえると、DeveloperAgentの双方向統一実装は12-2-3章の実装レベルの
 * 記述に忠実であり、5-2章の文言はboard項目のみ厳密さを欠いた表現だった可能性が高いと判断する。
 * ただし、5-2章の文言（board=片方向的表現）とプロダクトオーナー確認済み事項（10章#1）が直接
 * 「board除外の方向性」を明示的に確認したとまでは言い切れないため、**バグとしての差し戻しはしないが、
 * 5-2章のboard項目の表現を12-2-3章の実装と一致するよう「ブロック関係（双方向）にあるユーザーの投稿を
 * 除外」に修正することをArchitectAgentに推奨する（表現の精緻化、機能修正ではない）**。
 * 以下のテストは、DeveloperAgentの実装が実際に「双方向」で動作していることを確認する目的で書く
 * （Pass=現状の双方向実装を確認）。
 */
describe("ブロックによる遡及フィルタ", () => {
  const app = createApp();

  describe("GET /round-events からの除外", () => {
    test("自分がブロックした相手の募集は除外される", async () => {
      const me = await registerNewUser(app);
      const organizer = await registerNewUser(app);
      const event = await request(app)
        .post("/round-events")
        .set(...authHeader(organizer.accessToken))
        .send({ club_name: "テストGC", datetime: "2026-09-01T09:00:00+09:00", fee: 5000, capacity: 4 })
        .expect(201);

      await request(app)
        .post(`/users/${organizer.userId}/block`)
        .set(...authHeader(me.accessToken))
        .expect(204);

      const res = await request(app)
        .get("/round-events")
        .set(...authHeader(me.accessToken))
        .expect(200);
      expect(res.body.find((e: { event_id: string }) => e.event_id === event.body.event_id)).toBeUndefined();
    });

    test("【双方向確認】相手から一方的にブロックされている場合も、その相手の募集は除外される", async () => {
      const organizer = await registerNewUser(app); // ブロックする側
      const viewer = await registerNewUser(app); // ブロックされる側(自分からは誰もブロックしていない)
      const event = await request(app)
        .post("/round-events")
        .set(...authHeader(organizer.accessToken))
        .send({ club_name: "テストGC2", datetime: "2026-09-02T09:00:00+09:00", fee: 5000, capacity: 4 })
        .expect(201);

      await request(app)
        .post(`/users/${viewer.userId}/block`)
        .set(...authHeader(organizer.accessToken))
        .expect(204);

      const res = await request(app)
        .get("/round-events")
        .set(...authHeader(viewer.accessToken))
        .expect(200);
      expect(res.body.find((e: { event_id: string }) => e.event_id === event.body.event_id)).toBeUndefined();
    });
  });

  describe("GET /users/recommend からの除外", () => {
    async function seedMatchingPair(app: ReturnType<typeof createApp>) {
      const areaId = await seedArea();
      const userA = await registerNewUser(app, { areaId, averageScore: 100, purpose: "CASUAL" });
      const userB = await registerNewUser(app, { areaId, averageScore: 100, purpose: "CASUAL" });
      return { userA, userB };
    }

    test("前提確認: ブロックしていなければスコア60点以上のユーザーが推薦される", async () => {
      const { userA, userB } = await seedMatchingPair(app);
      const res = await request(app)
        .get("/users/recommend")
        .set(...authHeader(userA.accessToken))
        .expect(200);
      expect(res.body.find((u: { user_id: string }) => u.user_id === userB.userId)).toBeDefined();
    });

    test("自分がブロックした相手は推薦結果から除外される", async () => {
      const { userA, userB } = await seedMatchingPair(app);
      await request(app)
        .post(`/users/${userB.userId}/block`)
        .set(...authHeader(userA.accessToken))
        .expect(204);

      const res = await request(app)
        .get("/users/recommend")
        .set(...authHeader(userA.accessToken))
        .expect(200);
      expect(res.body.find((u: { user_id: string }) => u.user_id === userB.userId)).toBeUndefined();
    });

    test("【双方向確認】相手から一方的にブロックされている場合も推薦結果から除外される", async () => {
      const { userA, userB } = await seedMatchingPair(app);
      await request(app)
        .post(`/users/${userA.userId}/block`)
        .set(...authHeader(userB.accessToken))
        .expect(204);

      const res = await request(app)
        .get("/users/recommend")
        .set(...authHeader(userA.accessToken))
        .expect(200);
      expect(res.body.find((u: { user_id: string }) => u.user_id === userB.userId)).toBeUndefined();
    });
  });

  describe("GET /board からの除外", () => {
    test("自分がブロックした相手の投稿は除外される", async () => {
      const me = await registerNewUser(app);
      const author = await registerNewUser(app);
      const post = await request(app)
        .post("/board")
        .set(...authHeader(author.accessToken))
        .send({ content: "ブロック対象の投稿" })
        .expect(201);

      await request(app)
        .post(`/users/${author.userId}/block`)
        .set(...authHeader(me.accessToken))
        .expect(204);

      const res = await request(app)
        .get("/board")
        .set(...authHeader(me.accessToken))
        .expect(200);
      expect(res.body.find((p: { post_id: string }) => p.post_id === post.body.post_id)).toBeUndefined();
    });

    test(
      "【双方向確認・設計確認事項】相手から一方的にブロックされている場合も投稿は除外される" +
        "(技術設計書5-2章の文言は「ブロックした」という片方向的表現だが、実装は12-2-3章に従い双方向)",
      async () => {
        const author = await registerNewUser(app); // ブロックする側
        const viewer = await registerNewUser(app); // ブロックされる側(自分からは誰もブロックしていない)
        const post = await request(app)
          .post("/board")
          .set(...authHeader(author.accessToken))
          .send({ content: "双方向確認用の投稿" })
          .expect(201);

        await request(app)
          .post(`/users/${viewer.userId}/block`)
          .set(...authHeader(author.accessToken))
          .expect(204);

        const res = await request(app)
          .get("/board")
          .set(...authHeader(viewer.accessToken))
          .expect(200);
        expect(res.body.find((p: { post_id: string }) => p.post_id === post.body.post_id)).toBeUndefined();
      }
    );
  });

  describe("POST /users/{id}/match-requests のブロック時拒否", () => {
    test("自分が相手をブロックしていると申請は403 BLOCKEDを返す", async () => {
      const from = await registerNewUser(app);
      const to = await registerNewUser(app);
      await request(app)
        .post(`/users/${to.userId}/block`)
        .set(...authHeader(from.accessToken))
        .expect(204);

      const res = await request(app)
        .post(`/users/${to.userId}/match-requests`)
        .set(...authHeader(from.accessToken))
        .expect(403);
      expect(res.body.error.code).toBe("BLOCKED");
    });

    test("相手が自分をブロックしていると申請は403 BLOCKEDを返す(双方向)", async () => {
      const from = await registerNewUser(app);
      const to = await registerNewUser(app);
      await request(app)
        .post(`/users/${from.userId}/block`)
        .set(...authHeader(to.accessToken))
        .expect(204);

      const res = await request(app)
        .post(`/users/${to.userId}/match-requests`)
        .set(...authHeader(from.accessToken))
        .expect(403);
      expect(res.body.error.code).toBe("BLOCKED");
    });
  });

  describe("POST /round-events/{id}/join-requests のブロック時拒否", () => {
    test("主催者が申請者をブロックしていると参加申請は403 BLOCKEDを返す", async () => {
      const organizer = await registerNewUser(app);
      const applicant = await registerNewUser(app);
      const event = await request(app)
        .post("/round-events")
        .set(...authHeader(organizer.accessToken))
        .send({ club_name: "ブロックGC", datetime: "2026-09-03T09:00:00+09:00", fee: 5000, capacity: 4 })
        .expect(201);
      await request(app)
        .post(`/users/${applicant.userId}/block`)
        .set(...authHeader(organizer.accessToken))
        .expect(204);

      const res = await request(app)
        .post(`/round-events/${event.body.event_id}/join-requests`)
        .set(...authHeader(applicant.accessToken))
        .expect(403);
      expect(res.body.error.code).toBe("BLOCKED");
    });

    test("申請者が主催者をブロックしていても参加申請は403 BLOCKEDを返す(双方向)", async () => {
      const organizer = await registerNewUser(app);
      const applicant = await registerNewUser(app);
      const event = await request(app)
        .post("/round-events")
        .set(...authHeader(organizer.accessToken))
        .send({ club_name: "ブロックGC2", datetime: "2026-09-04T09:00:00+09:00", fee: 5000, capacity: 4 })
        .expect(201);
      await request(app)
        .post(`/users/${organizer.userId}/block`)
        .set(...authHeader(applicant.accessToken))
        .expect(204);

      const res = await request(app)
        .post(`/round-events/${event.body.event_id}/join-requests`)
        .set(...authHeader(applicant.accessToken))
        .expect(403);
      expect(res.body.error.code).toBe("BLOCKED");
    });
  });
});
