package com.golfmatch.app.data.api

import java.io.IOException

/**
 * サーバーが返したエラーレスポンス（技術設計書12-6章の`{ "error": { "code", "message" } }`形式）を
 * 表す例外。
 *
 * 各ViewModelは`runCatching { ... }.onFailure { error -> ... error.message ... }`という共通パターンで
 * エラーを画面に出しているため、[message]にはそのまま表示できる日本語メッセージを入れる。
 * これが無いと Retrofit の`HttpException`の既定メッセージ（`HTTP 400 Bad Request`）が
 * そのままユーザーに見えてしまう。
 *
 * OkHttp/Retrofitの経路をそのまま伝播させるため[IOException]を継承している
 * （非IOExceptionはOkHttp内部で別扱いになりうるため）。
 *
 * [code]は技術設計書12-6章のエラーコード（`VALIDATION_ERROR` / `UNAUTHENTICATED` / `FORBIDDEN` /
 * `NOT_FOUND` / `CONFLICT` / `BLOCKED` / `RATE_LIMITED` / `INTERNAL`）。レスポンスから読み取れなかった
 * 場合はnull。将来「未認証なら再ログイン画面へ」のような分岐が必要になった際の判定材料として保持する。
 */
class ApiException(
    val httpStatus: Int,
    val code: String?,
    override val message: String
) : IOException(message)

/**
 * 通信そのものに失敗した場合（圏外・タイムアウト・DNS解決失敗等）の例外。
 *
 * 元の[IOException]のメッセージは`Unable to resolve host "..."`のような英語の技術的文言であり
 * ユーザーには意味を成さないため、表示用の日本語メッセージに差し替える。
 */
class NetworkException(
    override val message: String,
    override val cause: Throwable?
) : IOException(message, cause)
