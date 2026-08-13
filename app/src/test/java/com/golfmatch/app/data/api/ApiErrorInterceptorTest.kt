package com.golfmatch.app.data.api

import com.golfmatch.app.data.api.ApiErrorInterceptor.Companion.extractErrorCode
import com.golfmatch.app.data.api.ApiErrorInterceptor.Companion.extractErrorMessage
import com.golfmatch.app.data.api.ApiErrorInterceptor.Companion.fallbackMessage
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * [ApiErrorInterceptor]のテスト（技術設計書12-6章）。
 *
 * サーバーは`{ "error": { "code", "message" } }`形式で日本語メッセージを返すが、変換しないと
 * ViewModelが表示する`error.message`はRetrofitの`HTTP 400 Bad Request`のような生の文言になる。
 * 全画面のエラー表示が通る共通経路のため、形式が崩れたレスポンスでも必ず表示可能な文言を返すことを重視する。
 */
class ApiErrorInterceptorTest {

    private val request = Request.Builder().url("https://example.com/api/areas").build()

    /** 指定のステータス・ボディを返すチェーンを組み立てて[ApiErrorInterceptor]を通す */
    private fun intercept(httpStatus: Int, body: String?): Response {
        val chain = object : Interceptor.Chain {
            override fun request(): Request = request
            override fun proceed(request: Request): Response = Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(httpStatus)
                .message("")
                .body((body ?: "").toResponseBody("application/json".toMediaType()))
                .build()

            override fun connection() = null
            override fun call() = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun readTimeoutMillis() = 0
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun writeTimeoutMillis() = 0
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }
        return ApiErrorInterceptor().intercept(chain)
    }

    @Test
    fun `サーバーのエラーメッセージがそのまま例外メッセージになる`() {
        val body = """{"error":{"code":"VALIDATION_ERROR","message":"phone_numberはE.164形式で指定してください"}}"""

        val error = assertThrows(ApiException::class.java) { intercept(400, body) }

        assertEquals("phone_numberはE.164形式で指定してください", error.message)
        assertEquals("VALIDATION_ERROR", error.code)
        assertEquals(400, error.httpStatus)
    }

    @Test
    fun `成功レスポンスはそのまま素通しする`() {
        val response = intercept(200, """[{"area_id":"area-1"}]""")

        assertTrue(response.isSuccessful)
        assertEquals("""[{"area_id":"area-1"}]""", response.body?.string())
    }

    @Test
    fun `エラー形式で返ってこない場合はステータスに応じた日本語メッセージにする`() {
        // Cloud Functions自体が落ちている場合などはHTMLやプレーンテキストが返りうる
        val error = assertThrows(ApiException::class.java) { intercept(502, "<html>Bad Gateway</html>") }

        assertEquals("サーバーでエラーが発生しました。時間をおいてもう一度お試しください", error.message)
        assertNull(error.code)
    }

    @Test
    fun `ボディが空でもメッセージは空にならない`() {
        val error = assertThrows(ApiException::class.java) { intercept(401, "") }

        assertEquals("ログインの有効期限が切れました。もう一度ログインしてください", error.message)
    }

    @Test
    fun `通信エラーは電波状況を促す日本語メッセージに変換される`() {
        val chain = object : Interceptor.Chain {
            override fun request(): Request = request
            override fun proceed(request: Request): Response = throw IOException("Unable to resolve host \"example.com\"")
            override fun connection() = null
            override fun call() = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 0
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun readTimeoutMillis() = 0
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun writeTimeoutMillis() = 0
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }

        val error = assertThrows(NetworkException::class.java) { ApiErrorInterceptor().intercept(chain) }

        assertEquals(ApiErrorInterceptor.NETWORK_ERROR_MESSAGE, error.message)
        // 元の例外は原因として保持する（ログ・調査用）
        assertTrue(error.cause is IOException)
    }

    // --- パース・フォールバックの境界値 ---

    @Test
    fun `エラーメッセージの抽出は形式が崩れていてもnullを返すだけで例外にしない`() {
        assertNull(extractErrorMessage(null))
        assertNull(extractErrorMessage(""))
        assertNull(extractErrorMessage("not json"))
        assertNull(extractErrorMessage("[1,2,3]"))
        assertNull(extractErrorMessage("""{"error":"文字列のerror"}"""))
        assertNull(extractErrorMessage("""{"error":{"code":"NOT_FOUND"}}"""))
        assertNull(extractErrorMessage("""{"error":{"message":""}}"""))
        assertNull(extractErrorCode("""{"error":{"message":"見つかりません"}}"""))
    }

    @Test
    fun `フォールバックメッセージはステータスごとに使い分ける`() {
        assertEquals("この操作を行う権限がありません", fallbackMessage(403))
        assertEquals("対象が見つかりませんでした", fallbackMessage(404))
        assertEquals("リクエストが多すぎます。しばらく待ってからお試しください", fallbackMessage(429))
        assertEquals("サーバーの応答がありませんでした。時間をおいてもう一度お試しください", fallbackMessage(504))
        assertEquals("エラーが発生しました（418）", fallbackMessage(418))
    }
}
