package com.golfmatch.app.data.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 現在のアクセストークンを保持するシングルトン（ADR-0003）。
 *
 * [NetworkModule] のInterceptorが全APIリクエストへの `Authorization: Bearer` 付与に、
 * [AuthRepositoryImpl] がログイン・登録成功時のトークン保存に利用する。
 *
 * 現時点ではプロセス内メモリ保持のみ（アプリ再起動でログアウトされる）。
 * アプリ終了後もセッションを維持する永続化（DataStore等）は次フェーズで検討する。
 */
@Singleton
class AuthSessionManager @Inject constructor() {
    @Volatile
    var accessToken: String? = null
        private set

    fun updateToken(token: String?) {
        accessToken = token
    }

    fun clear() {
        accessToken = null
    }
}
