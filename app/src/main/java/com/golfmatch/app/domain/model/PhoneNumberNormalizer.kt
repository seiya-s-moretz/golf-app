package com.golfmatch.app.domain.model

/**
 * ユーザーが入力した電話番号を、サーバーが要求するE.164形式へ正規化する（技術設計書6-1章）。
 *
 * サーバーの`POST /auth/phone/otp`・`POST /auth/phone/verify`は`^\+[1-9]\d{1,14}$`（E.164）でしか
 * 受け付けない（`functions/src/modules/auth/auth.validation.ts`）。一方、日本のユーザーが自然に入力するのは
 * `09012345678`や`090-1234-5678`といった国内表記であり、そのまま送ると400になる。
 * その差をここで吸収する。
 *
 * 対象は日本国内の番号（初期展開エリアは東京23区・川崎市・横浜市。技術設計書10章#5）に限定し、
 * 先頭`0`を国番号`+81`に置き換える。`+`で始まる入力は既にE.164とみなしてそのまま扱う。
 */
object PhoneNumberNormalizer {

    private val E164_REGEX = Regex("""^\+[1-9]\d{1,14}$""")

    /** 入力に含まれても無視する区切り文字（ハイフン・空白・括弧・全角ハイフン） */
    private val SEPARATORS = Regex("""[\s\-－―ー()（）]""")

    private const val JAPAN_COUNTRY_CODE = "+81"

    /**
     * [input]をE.164形式へ正規化する。正規化できない場合はnullを返す。
     *
     * - `090-1234-5678` / `090 1234 5678` → `+819012345678`（先頭0を`+81`に置換）
     * - `+819012345678` → そのまま
     * - 空文字・数字以外を含む・桁数がE.164の範囲外 → null
     */
    fun normalizeOrNull(input: String): String? {
        val compact = SEPARATORS.replace(input.trim(), "")
        if (compact.isEmpty()) return null

        val e164 = when {
            compact.startsWith("+") -> compact
            // 国内表記（先頭0）。0を除いた残りに国番号を付ける
            compact.startsWith("0") -> JAPAN_COUNTRY_CODE + compact.drop(1)
            else -> return null
        }
        return e164.takeIf { E164_REGEX.matches(it) }
    }
}
