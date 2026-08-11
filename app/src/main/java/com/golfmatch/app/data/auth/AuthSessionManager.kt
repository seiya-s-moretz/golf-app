package com.golfmatch.app.data.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 現在のアクセストークン・ログイン中ユーザーIDを保持するシングルトン（ADR-0003）。
 *
 * [NetworkModule] のInterceptorが全APIリクエストへの `Authorization: Bearer` 付与に、
 * [AuthRepositoryImpl] がログイン・登録成功時のトークン・ユーザーID保存に利用する。
 * マイページ画面（[com.golfmatch.app.ui.viewmodel.MyPageViewModel]）は「ログイン中の自分」の
 * プロフィールを取得・更新する必要があるが、`GET /users/me` のようなエンドポイントは技術設計書に
 * 定義されていないため、ログイン・登録レスポンス（`AuthSession.userId`）で得たユーザーIDを
 * ここに保持し、以後 `GET /users/{id}` の `{id}` として利用する（既存パターンの範囲内での実装判断）。
 *
 * 現時点ではプロセス内メモリ保持のみ（アプリ再起動でログアウトされる）。
 * アプリ終了後もセッションを維持する永続化（DataStore等）は次フェーズで検討する。
 */
@Singleton
class AuthSessionManager @Inject constructor() {
    @Volatile
    var accessToken: String? = null
        private set

    @Volatile
    var currentUserId: String? = null
        private set

    fun updateSession(accessToken: String?, userId: String?) {
        this.accessToken = accessToken
        this.currentUserId = userId
    }

    fun clear() {
        accessToken = null
        currentUserId = null
    }
}
