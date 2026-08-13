package com.golfmatch.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 電話番号のE.164正規化のテスト（技術設計書6-1章）。
 *
 * サーバー側は`^\+[1-9]\d{1,14}$`でしか受け付けない（`functions/src/modules/auth/auth.validation.ts`）。
 * 画面のプレースホルダは`090xxxxxxxx`（国内表記）であり、この変換が無いと入力どおりに操作した
 * ユーザー全員が400で弾かれる。
 */
class PhoneNumberNormalizerTest {

    @Test
    fun `国内表記は先頭0を国番号に置き換える`() {
        assertEquals("+819012345678", PhoneNumberNormalizer.normalizeOrNull("09012345678"))
    }

    @Test
    fun `ハイフンや空白の区切りは無視される`() {
        assertEquals("+819012345678", PhoneNumberNormalizer.normalizeOrNull("090-1234-5678"))
        assertEquals("+819012345678", PhoneNumberNormalizer.normalizeOrNull("090 1234 5678"))
        assertEquals("+819012345678", PhoneNumberNormalizer.normalizeOrNull("  09012345678  "))
    }

    @Test
    fun `既にE164形式ならそのまま返す`() {
        assertEquals("+819012345678", PhoneNumberNormalizer.normalizeOrNull("+819012345678"))
        assertEquals("+819012345678", PhoneNumberNormalizer.normalizeOrNull("+81 90-1234-5678"))
    }

    @Test
    fun `固定電話の国内表記も変換できる`() {
        assertEquals("+81312345678", PhoneNumberNormalizer.normalizeOrNull("03-1234-5678"))
    }

    @Test
    fun `空文字や区切り文字だけの入力はnull`() {
        assertNull(PhoneNumberNormalizer.normalizeOrNull(""))
        assertNull(PhoneNumberNormalizer.normalizeOrNull("   "))
        assertNull(PhoneNumberNormalizer.normalizeOrNull("---"))
    }

    @Test
    fun `0でも+でも始まらない入力はnull`() {
        assertNull(PhoneNumberNormalizer.normalizeOrNull("9012345678"))
        assertNull(PhoneNumberNormalizer.normalizeOrNull("819012345678"))
    }

    @Test
    fun `数字以外を含む入力はnull`() {
        assertNull(PhoneNumberNormalizer.normalizeOrNull("090abc5678"))
        assertNull(PhoneNumberNormalizer.normalizeOrNull("090*1234#5678"))
    }

    @Test
    fun `E164の桁数上限を超える入力はnull`() {
        // +81 + 15桁 = E.164の最大15桁を超える
        assertNull(PhoneNumberNormalizer.normalizeOrNull("0123456789012345"))
    }
}
