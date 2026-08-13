package com.golfmatch.app.data.api

import com.google.gson.JsonParser
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * エラーレスポンス・通信エラーを、画面にそのまま表示できる例外へ変換するInterceptor（技術設計書12-6章）。
 *
 * 変換しない場合、各ViewModelが表示する`error.message`は Retrofit の`HttpException`の既定メッセージ
 * （`HTTP 400 Bad Request`）や`Unable to resolve host ...`といった生の文言になる。サーバーは
 * `{ "error": { "code", "message" } }`形式で日本語メッセージを返しているため、ここで拾って
 * [ApiException]に載せ替える。全APIを通る共通経路のため、ViewModel側の変更は不要。
 */
@Singleton
class ApiErrorInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = try {
            chain.proceed(chain.request())
        } catch (e: ApiException) {
            throw e
        } catch (e: IOException) {
            throw NetworkException(NETWORK_ERROR_MESSAGE, e)
        }

        if (response.isSuccessful) return response

        // 以降必ず例外を投げるため、ボディを読み切って閉じてしまってよい
        val bodyText = runCatching { response.body?.string() }.getOrNull()
        response.close()
        throw ApiException(
            httpStatus = response.code,
            code = extractErrorCode(bodyText),
            message = extractErrorMessage(bodyText) ?: fallbackMessage(response.code)
        )
    }

    companion object {
        const val NETWORK_ERROR_MESSAGE = "通信に失敗しました。電波状況を確認してもう一度お試しください"

        /** `{ "error": { "message": "..." } }`からメッセージを取り出す。形式が異なる場合はnull */
        fun extractErrorMessage(bodyText: String?): String? = parseErrorObject(bodyText, "message")

        /** `{ "error": { "code": "..." } }`からエラーコードを取り出す。形式が異なる場合はnull */
        fun extractErrorCode(bodyText: String?): String? = parseErrorObject(bodyText, "code")

        private fun parseErrorObject(bodyText: String?, field: String): String? {
            if (bodyText.isNullOrBlank()) return null
            return runCatching {
                JsonParser.parseString(bodyText)
                    .takeIf { it.isJsonObject }
                    ?.asJsonObject
                    ?.getAsJsonObject("error")
                    ?.get(field)
                    ?.takeIf { it.isJsonPrimitive }
                    ?.asString
                    ?.takeIf { it.isNotBlank() }
            }.getOrNull()
        }

        /**
         * サーバーのエラー形式で返ってこなかった場合（例: Cloud Functions自体が落ちている、
         * インフラ層が返す502/504など）の表示用メッセージ。
         */
        fun fallbackMessage(httpStatus: Int): String = when (httpStatus) {
            401 -> "ログインの有効期限が切れました。もう一度ログインしてください"
            403 -> "この操作を行う権限がありません"
            404 -> "対象が見つかりませんでした"
            408, 504 -> "サーバーの応答がありませんでした。時間をおいてもう一度お試しください"
            429 -> "リクエストが多すぎます。しばらく待ってからお試しください"
            in 500..599 -> "サーバーでエラーが発生しました。時間をおいてもう一度お試しください"
            else -> "エラーが発生しました（$httpStatus）"
        }
    }
}
