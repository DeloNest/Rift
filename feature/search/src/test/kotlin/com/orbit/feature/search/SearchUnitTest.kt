package com.orbit.feature.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchUnitTest {

    @Test
    fun testArabicNormalizer_tashkeel() {
        val input = "قُرْآنٌ كَرِيمٌ"
        val normalized = ArabicNormalizer.normalize(input)
        assertEquals("قران كريم", normalized)
    }

    @Test
    fun testArabicNormalizer_tatweel() {
        val input = "تــــطــــبــــيــــق"
        val normalized = ArabicNormalizer.normalize(input)
        assertEquals("تطبيق", normalized)
    }

    @Test
    fun testArabicNormalizer_alefVariations() {
        assertEquals("احمد", ArabicNormalizer.normalize("أحمد"))
        assertEquals("ابراهيم", ArabicNormalizer.normalize("إبراهيم"))
        assertEquals("ايه", ArabicNormalizer.normalize("آية"))
        assertEquals("اسم", ArabicNormalizer.normalize("ٱسم"))
    }

    @Test
    fun testArabicNormalizer_taaMarbutaAndYaa() {
        assertEquals("ساعه", ArabicNormalizer.normalize("ساعة"))
        assertEquals("مني", ArabicNormalizer.normalize("منى"))
    }

    @Test
    fun testArabicNormalizer_easternNumerals() {
        assertEquals("2026", ArabicNormalizer.normalize("٢٠٢٦"))
        assertEquals("123", ArabicNormalizer.normalize("١٢٣"))
    }

    @Test
    fun testArabicNormalizer_initials() {
        val initialsArabic = ArabicNormalizer.extractInitials("خرائط جوجل")
        assertEquals("خج", initialsArabic)

        val initialsEnglish = ArabicNormalizer.extractInitials("google maps")
        assertEquals("gm", initialsEnglish)
    }

    @Test
    fun testFuzzyMatcher_exactAndPrefix() {
        val exact = FuzzyMatcher.score("whatsapp", "whatsapp")
        assertEquals(FuzzyMatcher.SCORE_EXACT, exact)

        val prefix = FuzzyMatcher.score("what", "whatsapp")
        assertTrue(prefix > FuzzyMatcher.SCORE_WORD_PREFIX)
    }

    @Test
    fun testFuzzyMatcher_damerauLevenshtein() {
        // Transposition distance is 1
        val dist = FuzzyMatcher.damerauLevenshtein("wathsapp", "whatsapp")
        assertEquals(1, dist)

        // Matches fuzzy
        val score = FuzzyMatcher.score("wathsapp", "whatsapp")
        assertTrue(score > FuzzyMatcher.SCORE_NONE)
    }
}
