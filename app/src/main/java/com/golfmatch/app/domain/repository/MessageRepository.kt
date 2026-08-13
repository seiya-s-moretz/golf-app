package com.golfmatch.app.domain.repository

import com.golfmatch.app.domain.model.Conversation
import com.golfmatch.app.domain.model.Message

/**
 * メッセージのリポジトリ（技術設計書 6-7章、ADR-0004）
 */
interface MessageRepository {
    /**
     * 会話一覧（最終更新順）。[before]・[beforeId]はページネーションカーソル（前ページ末尾の
     * `updatedAt`と`conversationId`。nullなら先頭から）。
     * ブロック除外はサーバー側で取得後に行われるため、返却件数が[limit]未満でも次ページが存在しうる。
     */
    suspend fun getConversations(before: String?, beforeId: String?, limit: Int): List<Conversation>

    /**
     * [partnerId] とのメッセージ履歴取得。Connectionが存在しない場合はサーバー側で403となる。
     * [before] はページネーションカーソル（nullの場合は最新から取得）。
     */
    suspend fun getMessages(partnerId: String, before: String?, beforeId: String?, limit: Int): List<Message>

    /** Connectionが存在しない、またはブロック関係にある場合はサーバー側で拒否される */
    suspend fun sendMessage(partnerId: String, content: String): Message

    suspend fun markAsRead(partnerId: String)
}
