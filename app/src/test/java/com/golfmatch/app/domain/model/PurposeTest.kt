package com.golfmatch.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * [Purpose] enumのテスト。
 *
 * 技術設計書5-1章: purpose は enum（わいわい／ガチ／レクチャー求）。
 * [Purpose.fromWireValue] はサーバーからのenum名・日本語ラベルいずれの表現も解決できる必要がある
 * （UserMapper / RegisterUserRequestDto 等で利用）。
 */
class PurposeTest {

    @Test
    fun `fromWireValue はenum名から解決できる`() {
        assertEquals(Purpose.CASUAL, Purpose.fromWireValue("CASUAL"))
        assertEquals(Purpose.SERIOUS, Purpose.fromWireValue("SERIOUS"))
        assertEquals(Purpose.LESSON_WANTED, Purpose.fromWireValue("LESSON_WANTED"))
    }

    @Test
    fun `fromWireValue は日本語ラベルからも解決できる`() {
        assertEquals(Purpose.CASUAL, Purpose.fromWireValue("わいわい"))
        assertEquals(Purpose.SERIOUS, Purpose.fromWireValue("ガチ"))
        assertEquals(Purpose.LESSON_WANTED, Purpose.fromWireValue("レクチャー求"))
    }

    @Test
    fun `fromWireValue は未知の値でIllegalArgumentExceptionを投げる`() {
        assertThrows(IllegalArgumentException::class.java) {
            Purpose.fromWireValue("恋愛目的")
        }
    }

    @Test
    fun `目的タグは3種のみで恋愛要素を含まない(PRD 0章)`() {
        val labels = Purpose.entries.map { it.label }.toSet()
        assertEquals(setOf("わいわい", "ガチ", "レクチャー求"), labels)
        assertEquals(3, Purpose.entries.size)
    }
}
