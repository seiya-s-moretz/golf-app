# ADR-0001: ラウンド参加を「即時加算」から「申請→承認」フローへ明文化する

## ステータス
承認（ArchitectAgent決定。ただし技術設計書10章#3のとおり、PM/ユーザーへの確認を推奨）

## コンテキスト
既存技術設計（`D:\勉強\golf\詳細設計書.md`）では、ラウンド募集への参加は以下のAPIで表現されていた。

```
POST /round-events/{id}/join
- 募集参加
- バリデーション：capacity > current
```

このAPI仕様上は、参加操作が行われた時点で即座に `current`（現在人数）が加算される「即時参加」として実装可能な設計になっている。一方、既存の基本設計書・設計書の画面仕様では、一貫して「募集への**参加申請**」という文言が使われており、UI上は「申請」という行為として説明されている。

PRD（`docs/要件定義書.md`）は、新規追加のメッセージ機能について「ラウンド募集の参加承認後に、該当ユーザー間でメッセージのやり取りができること」と明記している。これは、参加に「承認」という明示的なステップが存在し、承認前後で状態が区別できることを前提としている。既存のAPI仕様（即時加算）のままでは、「承認」という状態遷移が存在しないため、PRDの要求を満たせない。

## 決定
ラウンド参加を、以下の申請→承認ワークフローとして再定義する。

1. 参加希望者が `POST /round-events/{id}/join-requests` で参加申請を作成する（`RoundJoinRequest.status = PENDING`）。この時点では `RoundEvent.current` は加算しない。
2. 募集の主催者（`RoundEvent.created_by`）のみが `GET /round-events/{id}/join-requests` で申請一覧を確認できる。
3. 主催者が `POST /round-events/{id}/join-requests/{requestId}/approve` で承認すると、`capacity > current` を再検証したうえで `current` を加算し、`RoundJoinRequest.status = APPROVED` とする。同時に主催者⇔申請者間の `Connection` を作成し、メッセージ利用を可能にする。
4. 主催者は `.../reject` で却下することもできる。

既存の `POST /round-events/{id}/join` エンドポイントは `POST /round-events/{id}/join-requests` に置き換える。

## 代替案（不採用）
### 案: 即時参加のまま維持し、メッセージ機能は「参加後」（承認概念なし）に開放する
既存API仕様を変更せずに済むが、PRDが明示的に「承認後」と記載していること、既存UI文言が「申請」であることと整合しないため不採用。また、即時参加のままだと、主催者の意図しない相手が定員に達するまで無条件に参加できてしまい、安全性の観点（PRDの安全機能強化の方針）とも整合しない。

## 影響
- 既存API `POST /round-events/{id}/join` の挙動が変更される（互換性のない変更）。DeveloperAgentは新規実装のため影響は限定的だが、念のためPM/ユーザーに意図した挙動か確認を推奨する（技術設計書10章#3）。
- `RoundEvent.current` の更新契機が「申請時」から「承認時」に変わる。
- 新規エンティティ `RoundJoinRequest` が必要になる（技術設計書5-2章）。

## 関連
- `docs/技術設計書.md` 5-1章（RoundEvent）、5-2章（RoundJoinRequest）、6-4章（API）
